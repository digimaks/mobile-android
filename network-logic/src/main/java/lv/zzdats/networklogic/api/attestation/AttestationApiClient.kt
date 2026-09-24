// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.api.attestation

import android.util.Log
import lv.zzdats.businesslogic.controller.crypto.SecureAreaRepository
import lv.zzdats.networklogic.api.base.BaseApiClient
import lv.zzdats.networklogic.error.ApiErrorHandler
import lv.zzdats.networklogic.model.attestation.InstanceRq
import lv.zzdats.networklogic.model.attestation.NonceResp
import lv.zzdats.networklogic.model.attestation.toInstanceRequest
import retrofit2.Response

interface AttestationApiClient {
    suspend fun getNonce(): Result<NonceResp>
    suspend fun getInstance(nonce: String): Result<Unit>
    suspend fun checkInstance(): Boolean
    suspend fun submitInstance(request: InstanceRq): Result<Unit>
}

class AttestationApiClientImpl(
    private val attestationApi: AttestationApi,
    private val errorHandler: ApiErrorHandler,
    private val secureAreaRepository: SecureAreaRepository,
) : BaseApiClient(errorHandler), AttestationApiClient {
    private companion object {
        const val TAG = "AttestationApiClient"
    }

    override suspend fun getNonce(): Result<NonceResp> =
        handleRequest { attestationApi.getNonce() }

    override suspend fun getInstance(nonce: String): Result<Unit> =
        handleRequest {
            val firstResponse = submitGeneratedInstance(nonce, preferStrongBox = true)
            if (firstResponse.isCertificateValidationError()) {
                Log.w(
                    TAG,
                    "Certificate validation failed for StrongBox attestation, retrying with StrongBox disabled"
                )
                secureAreaRepository.deleteWalletInstance()
                submitGeneratedInstance(nonce, preferStrongBox = false)
            } else {
                firstResponse
            }
        }.onFailure {
            // ensure cleanup if the HTTP layer reported failure
            secureAreaRepository.deleteWalletInstance()
        }

    override suspend fun checkInstance(): Boolean {
        return secureAreaRepository.checkInstance()
    }

    override suspend fun submitInstance(request: InstanceRq): Result<Unit> =
        handleRequest { attestationApi.getInstance(request) }

    private suspend fun submitGeneratedInstance(
        nonce: String,
        preferStrongBox: Boolean
    ): Response<Unit> {
        val attestationData = secureAreaRepository.generateKeyWithAttestation(
            nonce = nonce,
            preferStrongBox = preferStrongBox
        )
        val instanceRequest = attestationData?.toInstanceRequest()
            ?: error("Invalid attestation request")
        return attestationApi.getInstance(instanceRequest)
    }

    private fun Response<*>.isCertificateValidationError(): Boolean {
        if (isSuccessful || code() != 422) {
            return false
        }
        val body = runCatching { raw().peekBody(Long.MAX_VALUE).string().lowercase() }
            .getOrDefault("")
        return body.contains("key: 'certificate'") && body.contains("invalid")
    }
}
