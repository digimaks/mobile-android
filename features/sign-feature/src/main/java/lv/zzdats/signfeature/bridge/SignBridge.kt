// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.signfeature.bridge

import android.content.Intent
import android.content.ContentValues
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.WebResourceRequest
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import lv.zzdats.resourceslogic.bridge.SIGN
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.signfeature.BuildConfig
import lv.zzdats.signfeature.interactor.SignDocumentInteractor
import lv.zzdats.signfeature.interactor.SignDocumentInteractorPartialState
import lv.zzdats.signfeature.interactor.SignDownloadInteractorPartialState
import lv.zzdats.signfeature.interactor.ValidateContainerPartialState
import lv.zzdats.commonfeature.features.auth.EparakstsAuthLauncher
import lv.zzdats.signfeature.util.FilePickerHelper
import lv.zzdats.signfeature.util.getMimeTypeFromExtension
import lv.zzdats.signfeature.util.isContainerFile
import lv.zzdats.uilogic.navigation.NavigationCommand.ToWeb
import lv.zzdats.uilogic.navigation.WebNavigationService
import lv.zzdats.webbridge.UrlHandler
import lv.zzdats.webbridge.core.BaseBridge
import lv.zzdats.webbridge.core.BridgeRequest
import lv.zzdats.webbridge.core.BridgeResponse
import java.io.File
import android.provider.MediaStore
import android.content.Context

class SignBridge(
    private val signDocumentInteractor: SignDocumentInteractor,
    private val filePickerHelper: FilePickerHelper,
    private val navigationService: WebNavigationService,
    private val resourceProvider: ResourceProvider
) : BaseBridge(), UrlHandler {

    private var downloadedFile: File? = null
    private var contentType: String? = null
    private var currentSigningRequestId: String? = null
    private var pendingDocumentId: String? = null
    private var pendingIsESeal: Boolean = false
    private var pendingSourceFilePath: String? = null
    private var isHandlingSignResult: Boolean = false

    override fun getName() = SIGN.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when (request.function) {
            SIGN.PICK_FILES -> handlePickFiles(request)
            SIGN.GET_SIGNING_METHODS -> handleGetSigningMethods(request)
            SIGN.SIGN_DOCUMENT -> handleSignDocument(request)
            SIGN.DOWNLOAD_DOCUMENT -> handleDownloadSignedFile(request)
            SIGN.SHARE_DOCUMENT -> handleShareSignedFile(request)
            SIGN.CLOSE_SESSION -> handleCloseSession(request)
            SIGN.GET_SHARED_FILE -> handleGetSharedFile(request)
            SIGN.OPEN_FILE -> handlePreviewContainerFile(request)
            else -> createErrorResponse(request, "Unknown function")
        }
    }

    private fun handlePickFiles(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            try {
                val files = filePickerHelper.pickFiles()
                processAndValidateFiles(files, request)
            } catch (e: Exception) {
                emitEvent(createErrorResponse(request, e.message))
            }
        }
        return createSuccessResponse(request, null)
    }

    private fun handleGetSigningMethods(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            signDocumentInteractor.getSigningMethods().collect { methods ->
                emitEvent(createSuccessResponse(request, mapOf("methods" to methods)))
            }
        }
        return createSuccessResponse(request, null)
    }

    private fun handleGetSharedFile(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val sharedFilePath = data["filePath"] as? String
            ?: return createErrorResponse(request, "Missing filePath")

        coroutineScope.launch {
            try {
                val fileInfo = resolveSharedFile(sharedFilePath)
                processAndValidateFiles(listOf(fileInfo), request)
            } catch (e: Exception) {
                emitEvent(createErrorResponse(request, e.message))
            }
        }
        return createSuccessResponse(request, null)
    }

    private fun resolveSharedFile(sharedFilePath: String): FilePickerHelper.FileInfo {
        val normalizedPath = Uri.decode(sharedFilePath).trim()
        val parsedUri = Uri.parse(normalizedPath)
        return when (parsedUri.scheme?.lowercase()) {
            "content" -> filePickerHelper.processFileUri(parsedUri)
            "file" -> {
                val localPath = parsedUri.path ?: throw IllegalArgumentException("Invalid file URI")
                processLocalFile(File(localPath))
            }
            null -> processLocalFile(File(normalizedPath))
            else -> filePickerHelper.processFileUri(parsedUri)
        }
    }

    private fun processLocalFile(file: File): FilePickerHelper.FileInfo {
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("File not found: ${file.absolutePath}")
        }

        val extension = file.name.substringAfterLast('.', "")
        val fileSize = file.length()
        return FilePickerHelper.FileInfo(
            path = file.absolutePath,
            name = file.name,
            size = fileSize,
            mimeType = getMimeTypeFromExtension(extension),
            isContainer = isContainerFile(file),
            isValid = fileSize > 0 && fileSize <= FilePickerHelper.MAX_FILE_SIZE
        )
    }

    private suspend fun processAndValidateFiles(files: List<FilePickerHelper.FileInfo>, request: BridgeRequest) {
        val allowedExtensions = setOf("pdf", "asice", "edoc", "sce")
        val potentialContainerFile = files.firstOrNull { file ->
            file.isValid && allowedExtensions.contains(file.name.substringAfterLast('.').lowercase())
        }

        if (potentialContainerFile != null) {
            signDocumentInteractor.validateContainer(File(potentialContainerFile.path)).collect { state ->
                when (state) {
                    is ValidateContainerPartialState.Success -> {
                        val validFiles = state.files.filter { containerFile ->
                            containerFile.name.contains(".") && containerFile.name.substringAfterLast('.').isNotEmpty()
                        }

                        val isRealContainer = state.signers.isNotEmpty()

                        emitEvent(createSuccessResponse(request, mapOf(
                            "files" to files.map { file ->
                                mapOf(
                                    "path" to file.path,
                                    "name" to file.name,
                                    "size" to file.size,
                                    "type" to file.mimeType,
                                    "isContainer" to (file == potentialContainerFile && isRealContainer),
                                    "isValid" to file.isValid,
                                    "allowedOutputFormats" to determineAllowedOutputFormats(file.name),
                                    "containerInfo" to if (file == potentialContainerFile && isRealContainer) {
                                        mapOf(
                                            "signers" to state.signers.map { signer ->
                                                mapOf(
                                                    "name" to signer.name,
                                                    "signedAt" to signer.signedAt,
                                                    "type" to signer.type
                                                )
                                            },
                                            "files" to validFiles.map { containerFile ->
                                                mapOf(
                                                    "name" to containerFile.name,
                                                    "size" to containerFile.size
                                                )
                                            }
                                        )
                                    } else null
                                )
                            }
                        )))
                    }
                    is ValidateContainerPartialState.Failure -> {
                        emitEvent(createSuccessResponse(request, mapOf(
                            "files" to files.map { file ->
                                mapOf(
                                    "path" to file.path,
                                    "name" to file.name,
                                    "size" to file.size,
                                    "type" to file.mimeType,
                                    "isContainer" to file.isContainer,
                                    "isValid" to file.isValid,
                                    "allowedOutputFormats" to determineAllowedOutputFormats(file.name),
                                    "containerInfo" to null
                                )
                            },
                            "validationError" to state.error
                        )))
                    }
                }
            }
        } else {
            emitEvent(createSuccessResponse(request, mapOf(
                "files" to files.map { file ->
                    mapOf(
                        "path" to file.path,
                        "name" to file.name,
                        "size" to file.size,
                        "type" to file.mimeType,
                        "isContainer" to file.isContainer,
                        "isValid" to file.isValid,
                        "allowedOutputFormats" to determineAllowedOutputFormats(file.name),
                        "containerInfo" to null
                    )
                }
            )))
        }
    }

    private fun handlePreviewContainerFile(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val containerPath = data["containerPath"] as? String
            ?: return createErrorResponse(request, "Missing containerPath")

        val fileName = data["fileName"] as? String

        coroutineScope.launch {
            try {
                val context = resourceProvider.provideContext()
                val fileToOpen: File

                if (fileName != null) {
                    fileToOpen = filePickerHelper.extractFileFromContainer(containerPath, fileName)
                } else {
                    fileToOpen = File(containerPath)
                    if (!fileToOpen.exists()) {
                        throw IllegalStateException("File not found: $containerPath")
                    }
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    fileToOpen
                )

                val fileExtension = if (fileName != null) {
                    fileName.substringAfterLast('.')
                } else {
                    fileToOpen.name.substringAfterLast('.')
                }

                val mimeType = getMimeTypeFromExtension(fileExtension)

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(Intent.createChooser(intent, "Open with")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

                emitEvent(createSuccessResponse(request, null))
            } catch (e: Exception) {
                emitEvent(createErrorResponse(request, "Failed to open file: ${e.message}"))
            }
        }

        return createSuccessResponse(request, null)
    }

    override fun handleUrl(request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        if (EparakstsAuthLauncher.isEparakstsScheme(request.url.scheme)) {
            val uri = request.url
            val successUrl = uri.getQueryParameter("successurl")
            val failureUrl = uri.getQueryParameter("failureurl")

            val newBuilder = Uri.Builder()
                .scheme(uri.scheme)
                .authority(uri.authority)
                .path(uri.path)

            for (paramName in uri.queryParameterNames) {
                if (paramName != "successurl" && paramName != "failureurl") {
                    for (value in uri.getQueryParameters(paramName)) {
                        newBuilder.appendQueryParameter(paramName, value)
                    }
                }
            }

            newBuilder.appendQueryParameter(
                "successurl",
                "${BuildConfig.DEEPLINK}resume_sign?url=${Uri.encode(successUrl ?: "")}"
            )
            newBuilder.appendQueryParameter(
                "failureurl",
                "${BuildConfig.DEEPLINK}resume_sign?url=${Uri.encode(failureUrl ?: "")}"
            )

            val modifiedUri = newBuilder.build()

            val intent = Intent(Intent.ACTION_VIEW, modifiedUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                webView?.context?.startActivity(intent)
            } catch (_: Exception) {}
            return true
        }

        if (handleSignCallback(request.url)) {
            return true
        }

        return false
    }

    fun handleDeepLinkUri(uri: Uri): Boolean = handleSignCallback(uri)

    private fun handleSignCallback(uri: Uri): Boolean {
        val url = uri.toString()

        if (url.contains("resume_sign")) {
            val originalUrl = uri.getQueryParameter("url")
            if (originalUrl != null) {
                webView?.post {
                    webView?.loadUrl(originalUrl)
                }
            }
            return true
        }

        if (url.startsWith(BuildConfig.DEEPLINK)) {
            ensureSessionLoaded()
            val type = if (pendingIsESeal) "seal" else "esign"
            val pendingRouteFilePath = Uri.encode(pendingSourceFilePath ?: "null")
            if (url.contains("eseal-success")) {
                if (isHandlingSignResult) return true
                isHandlingSignResult = true

                coroutineScope.launch {
                    try {
                        pendingDocumentId?.let { docId ->
                            signDocumentInteractor.logSigningTransaction(
                                documentId = docId,
                                isSuccess = true,
                                isESeal = pendingIsESeal
                            )
                            pendingDocumentId = null
                        }
                        navigationService.navigate(ToWeb("sign-done/loading/$pendingRouteFilePath/$type"))

                        currentSigningRequestId?.let { requestId ->
                            signDocumentInteractor.downloadSignedDocument(requestId).collect { state ->
                                when (state) {
                                    is SignDownloadInteractorPartialState.Success -> {
                                        downloadedFile = state.file
                                        contentType = state.contentType
                                        persistDownloadedFile(state.file, state.contentType, requestId)
                                        val successRouteFilePath = Uri.encode(state.file.absolutePath)
                                        navigationService.navigate(
                                            ToWeb("sign-done/success/$successRouteFilePath/$type")
                                        )
                                    }
                                    is SignDownloadInteractorPartialState.Failure -> {
                                        navigationService.navigate(ToWeb("sign-done/error/$pendingRouteFilePath/$type"))
                                    }
                                }
                            }
                        }
                    } finally {
                        currentSigningRequestId = null
                        isHandlingSignResult = false
                        clearSignSession()
                    }
                }
                return true
            } else if (url.contains("eseal-error")) {
                if (isHandlingSignResult) return true
                isHandlingSignResult = true

                coroutineScope.launch {
                    try {
                        pendingDocumentId?.let { docId ->
                            signDocumentInteractor.logSigningTransaction(
                                documentId = docId,
                                isSuccess = false,
                                isESeal = pendingIsESeal
                            )
                            pendingDocumentId = null
                        }
                        navigationService.navigate(ToWeb("sign-done/error/$pendingRouteFilePath/$type"))
                    } finally {
                        currentSigningRequestId = null
                        isHandlingSignResult = false
                        clearSignSession()
                    }
                }
                return true
            }
            return true
        }

        return false
    }

    private fun handleSignDocument(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val filePath = data["filePath"] as? String
            ?: return createErrorResponse(request, "Missing filePath")

        val documentId = data["documentId"] as? String
            ?: return createErrorResponse(request, "Missing documentId")

        val outputFormat = data["outputFormat"] as? String
        val code = data["code"] as? String

        val file = File(filePath)
        if (!file.exists()) {
            return createErrorResponse(request, null)
        }
        val encodedSourceFilePath = Uri.encode(filePath)

        currentSigningRequestId = null
        downloadedFile = null
        pendingDocumentId = null
        pendingIsESeal = false
        pendingSourceFilePath = null
        isHandlingSignResult = false

        coroutineScope.launch {
            signDocumentInteractor.signDocument(
                file = file,
                documentId = documentId,
                outputFormat = outputFormat,
                code = code
            ).collect { state ->
                when (state) {
                    is SignDocumentInteractorPartialState.Success -> {
                        currentSigningRequestId = state.requestId
                        pendingDocumentId = documentId
                        pendingIsESeal = state.isESeal
                        pendingSourceFilePath = filePath
                        persistSignSession(state.requestId, documentId, state.isESeal, filePath)
                        clearDownloadedFile()
                        emitEvent(createSuccessResponse(request, null))

                        val ctx = webView?.context ?: resourceProvider.provideContext()
                        EparakstsAuthLauncher.launch(ctx, state.redirectUrl)
                    }
                    is SignDocumentInteractorPartialState.Failure -> {
                        navigationService.navigate(ToWeb("sign-done/error/$encodedSourceFilePath"))
                        clearSignSession()
                        emitEvent(createErrorResponse(request, state.error))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    private fun handleCloseSession(request: BridgeRequest): BridgeResponse {
        return createErrorResponse(request, "Not yet implemented")
    }

    private fun handleDownloadSignedFile(request: BridgeRequest): BridgeResponse {
        ensureDownloadedFileLoaded()
        downloadedFile?.let { tempFile ->
            val context = resourceProvider.provideContext()
            try {
                val result = saveToDownloads(context, tempFile, contentType ?: "application/edoc")
                result.fold(
                    onSuccess = {
                        emitEvent(createSuccessResponse(request, null))
                        return createSuccessResponse(request, null)
                    },
                    onFailure = { e ->
                        emitEvent(createErrorResponse(request, null))
                        return createErrorResponse(request, e.message)
                    }
                )
            } catch (e: Exception) {
                emitEvent(createErrorResponse(request, null))
                return createErrorResponse(request, e.message)
            }
        }
        return createErrorResponse(request, "signed_file_missing")
    }

    private fun handleShareSignedFile(request: BridgeRequest): BridgeResponse {
        ensureDownloadedFileLoaded()
        downloadedFile?.let { tempFile ->
            val context = resourceProvider.provideContext()
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = contentType ?: "application/edoc"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                emitEvent(createSuccessResponse(request, null))
                return createSuccessResponse(request, null)
            } catch (e: Exception) {
                emitEvent(createErrorResponse(request, null))
                return createErrorResponse(request, e.message)
            }
        }
        return createErrorResponse(request, "signed_file_missing")
    }

    private fun saveToDownloads(context: android.content.Context, sourceFile: File, mimeType: String): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + File.separator + "Digimaks"
                    )
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("download_insert_failed"))

                try {
                    resolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    } ?: return Result.failure(IllegalStateException("download_open_failed"))

                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    Result.success(Unit)
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    Result.failure(e)
                }
            } else {
                val downloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Digimaks"
                )
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }

                val destinationFile = File(downloadDir, sourceFile.name)
                var uniqueFile = destinationFile
                var counter = 1
                while (uniqueFile.exists()) {
                    val name = sourceFile.nameWithoutExtension
                    val ext = sourceFile.extension
                    uniqueFile = File(downloadDir, "${name}_${counter}.${ext}")
                    counter++
                }

                sourceFile.copyTo(uniqueFile)

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(uniqueFile.absolutePath),
                    null,
                    null
                )
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun signSessionPrefs(context: Context) =
        context.getSharedPreferences("sign_session", Context.MODE_PRIVATE)

    private fun downloadPrefs(context: Context) =
        context.getSharedPreferences("sign_download", Context.MODE_PRIVATE)

    private fun persistSignSession(requestId: String, documentId: String, isESeal: Boolean, sourceFilePath: String) {
        val context = resourceProvider.provideContext()
        signSessionPrefs(context).edit()
            .putString("requestId", requestId)
            .putString("documentId", documentId)
            .putBoolean("isESeal", isESeal)
            .putString("sourceFilePath", sourceFilePath)
            .apply()
    }

    private fun ensureSessionLoaded() {
        if (currentSigningRequestId != null) return
        val context = resourceProvider.provideContext()
        val prefs = signSessionPrefs(context)
        val storedRequestId = prefs.getString("requestId", null)
        if (storedRequestId.isNullOrBlank()) return
        currentSigningRequestId = storedRequestId
        pendingDocumentId = prefs.getString("documentId", null)
        pendingIsESeal = prefs.getBoolean("isESeal", false)
        pendingSourceFilePath = prefs.getString("sourceFilePath", null)
    }

    private fun clearSignSession() {
        val context = resourceProvider.provideContext()
        signSessionPrefs(context).edit().clear().apply()
        pendingSourceFilePath = null
    }

    private fun persistDownloadedFile(file: File, mimeType: String, requestId: String) {
        val context = resourceProvider.provideContext()
        downloadPrefs(context).edit()
            .putString("filePath", file.absolutePath)
            .putString("contentType", mimeType)
            .putString("requestId", requestId)
            .apply()
    }

    private fun ensureDownloadedFileLoaded() {
        if (downloadedFile != null) return
        val context = resourceProvider.provideContext()
        val prefs = downloadPrefs(context)
        val path = prefs.getString("filePath", null) ?: return
        val file = File(path)
        if (!file.exists()) return
        downloadedFile = file
        contentType = prefs.getString("contentType", contentType)
    }

    private fun clearDownloadedFile() {
        downloadedFile = null
        contentType = null
        val context = resourceProvider.provideContext()
        downloadPrefs(context).edit().clear().apply()
    }

    private fun determineAllowedOutputFormats(fileName: String): List<String> {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "pdf" -> listOf(".pdf", ".edoc")
            "edoc", "asice", "sce" -> listOf(".$extension")
            else -> listOf(".edoc")
        }
    }
}
