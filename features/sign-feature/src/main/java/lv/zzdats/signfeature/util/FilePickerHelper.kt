// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.signfeature.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

class FilePickerHelper(private val context: Context) {

    private var filePickerLauncher: ActivityResultLauncher<String>? = null
    private var pendingPickRequest: CompletableDeferred<List<FileInfo>>? = null

    data class FileInfo(
        val path: String,
        val name: String,
        val size: Long,
        val mimeType: String,
        val isContainer: Boolean,
        val isValid: Boolean
    )

    fun registerPicker(launcher: ActivityResultLauncher<String>) {
        filePickerLauncher = launcher
    }

    suspend fun pickFiles(): List<FileInfo> {
        if (filePickerLauncher == null) {
            throw IllegalStateException("File picker not registered")
        }

        val deferred = CompletableDeferred<List<FileInfo>>()
        pendingPickRequest = deferred

        filePickerLauncher?.launch("*/*")

        return deferred.await()
    }

    fun handlePickedFiles(uris: List<Uri>) {
        val currentRequest = pendingPickRequest ?: return

        try {
            val fileInfos = uris.mapNotNull { uri ->
                try {
                    processFileUri(uri)
                } catch (e: Exception) {
                    Log.e("FilePickerHelper", "Failed to process file: $uri, ${e.message}")
                    null
                }
            }
            currentRequest.complete(fileInfos)
        } catch (e: Exception) {
            currentRequest.completeExceptionally(e)
        }
    }

    suspend fun extractFileFromContainer(containerPath: String, entryName: String): File = withContext(Dispatchers.IO) {
        val containerFile = File(containerPath)
        val cacheDir = File(context.cacheDir, "container_preview")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val outputFile = File(cacheDir, entryName)

        try {
            ZipInputStream(containerFile.inputStream()).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(entryName)) {
                        outputFile.outputStream().use { output ->
                            zipStream.copyTo(output)
                        }
                        break
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }

            if (!outputFile.exists()) {
                throw IllegalStateException("File $entryName not found in container")
            }

            return@withContext outputFile
        } catch (e: Exception) {
            throw IllegalStateException("Failed to extract file: ${e.message}")
        }
    }

    fun processFileUri(uri: Uri): FileInfo {
        val cursor = context.contentResolver.query(uri, null, null, null, null)

        return cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

                val fileName = if (displayNameIndex != -1) it.getString(displayNameIndex) else "unknown"
                val fileSize = if (sizeIndex != -1) it.getLong(sizeIndex) else 0

                val file = copyUriToCache(uri, fileName)
                val mimeType = context.contentResolver.getType(uri) ?: getMimeTypeFromExtension(fileName.substringAfterLast('.', ""))
                val isContainer = isContainerFile(file)

                FileInfo(
                    path = file.absolutePath,
                    name = fileName,
                    size = fileSize,
                    mimeType = mimeType,
                    isContainer = isContainer,
                    isValid = fileSize > 0 && fileSize <= MAX_FILE_SIZE
                )
            } else {
                throw IllegalArgumentException("Invalid file URI")
            }
        } ?: throw IllegalArgumentException("Could not query file: $uri")
    }

    private fun copyUriToCache(uri: Uri, fileName: String): File {
        val cacheDir = File(context.cacheDir, "sign_files")
        cacheDir.mkdirs()

        val sanitizedName = fileName.ifBlank { "shared_file" }
        val file = uniqueFileInDir(cacheDir, sanitizedName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Could not open stream for: $uri")

        return file
    }

    private fun uniqueFileInDir(dir: File, fileName: String): File {
        val baseName = fileName.substringBeforeLast('.', fileName)
        val extension = fileName.substringAfterLast('.', "")

        var candidate = File(dir, fileName)
        var index = 1
        while (candidate.exists()) {
            val indexedName = if (extension.isBlank()) {
                "${baseName}_$index"
            } else {
                "${baseName}_$index.$extension"
            }
            candidate = File(dir, indexedName)
            index++
        }
        return candidate
    }

    companion object {
        const val MAX_FILE_SIZE = 26_214_400L // 25MB
    }
}

fun getMimeTypeFromExtension(extension: String): String {
    return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension.lowercase())
        ?: "application/octet-stream"
}

fun isContainerFile(file: File): Boolean {
    if (!file.exists() || file.length() < 4) {
        return false
    }

    return try {
        // Check PK signature
        file.inputStream().use { input ->
            val header = ByteArray(4)
            input.read(header)
            // PK\x03\x04 (0x504B0304)
            header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                    header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
        }
    } catch (e: Exception) {
        false
    }
}

fun isPDF(fileName: String): Boolean {
    return fileName.lowercase().endsWith(".pdf")
}
