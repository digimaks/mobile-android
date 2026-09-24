// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.repository

import kotlinx.serialization.json.JsonObject
import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

interface WalletAttestationRepository {

    suspend fun getWalletAttestation(
        baseUrl: String,
        keyInfo: JsonObject
    ): Result<String>

    suspend fun getKeyAttestation(
        baseUrl: String,
        keys: List<JsonObject>,
        nonce: String?,
        hardwareKeyTag: String?
    ): Result<String>
}

class WalletAttestationRepositoryImpl(
    private val okHttpClient: OkHttpClient,
) : WalletAttestationRepository {

    private companion object {
        const val WALLET_INSTANCE_ATTESTATION_PATH = "/wallet/wallet-instance-attestation/jwk?platform=android"
        const val WALLET_UNIT_ATTESTATION_PATH = "/wallet/wallet-unit-attestation/jwk-set?platform=android"
        private const val RETRY_DELAY_MS = 250L

        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun getWalletAttestation(
        baseUrl: String,
        keyInfo: JsonObject
    ): Result<String> = runCatching {
        val url = baseUrl.trimEnd('/') + WALLET_INSTANCE_ATTESTATION_PATH

        val body = buildJsonObject {
            put("jwk", keyInfo)
        }.toString()

        val responseText = postJson(url, body)
        val obj = json.parseToJsonElement(responseText).jsonObject

        obj["walletInstanceAttestation"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("No attestation response (walletInstanceAttestation missing)")
    }

    override suspend fun getKeyAttestation(
        baseUrl: String,
        keys: List<JsonObject>,
        nonce: String?,
        hardwareKeyTag: String?
    ): Result<String> = runCatching {
        val url = baseUrl.trimEnd('/') + WALLET_UNIT_ATTESTATION_PATH

        val body = buildJsonObject {
            nonce?.takeIf { it.isNotBlank() }?.let {
                put("nonce", JsonPrimitive(it))
            }
            hardwareKeyTag?.takeIf { it.isNotBlank() }?.let {
                put("hardwareKeyTag", JsonPrimitive(it))
            }
            put(
                "jwkSet",
                JsonObject(
                    mapOf(
                        "keys" to JsonArray(keys)
                    )
                )
            )
        }.toString()

        val responseText = postJson(url, body)
        val obj = json.parseToJsonElement(responseText).jsonObject

        obj["walletUnitAttestation"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("No attestation response (walletUnitAttestation missing)")
    }

    private suspend fun postJson(url: String, jsonBody: String): String {
        return try {
            executePostJson(url, jsonBody)
        } catch (e: IOException) {
            val isHttp2Cancel = e.message?.contains("stream was reset: CANCEL", ignoreCase = true) == true
            if (!isHttp2Cancel) throw e
            // Restarting the app clears OkHttp state and often fixes this.
            // Do the equivalent recovery in-process for transient broken HTTP/2 connections.
            okHttpClient.connectionPool.evictAll()
            delay(RETRY_DELAY_MS)
            executePostJson(
                url = url,
                jsonBody = jsonBody,
                forceHttp11 = true
            )
        }
    }

    private suspend fun executePostJson(
        url: String,
        jsonBody: String,
        forceHttp11: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val client = if (forceHttp11) {
            okHttpClient.newBuilder()
                .protocols(listOf(Protocol.HTTP_1_1))
                .build()
        } else {
            okHttpClient
        }

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string()
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code} calling $url; body=${body.orEmpty()}")
            }
            body ?: throw IOException("Empty HTTP body from $url")
        }
    }
}
