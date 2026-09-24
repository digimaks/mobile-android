// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.error

import android.util.Log
import java.net.SocketTimeoutException
import java.net.UnknownHostException


object ErrorUtils {
    const val ISSUANCE_TX_CODE_INVALID = "issuance_tx_code_invalid"
    const val ISSUANCE_TX_CODE_INACTIVE = "issuance_tx_code_inactive"
    const val ATTESTATION_CERTIFICATE_INVALID = "attestation_certificate_invalid"

    private val ALLOWED_ERROR_TAGS = setOf(
        "invalid",
        "toomanyattempts",
        "e164",
        // Signing
        "sign_not_found_eseal_signing_identity",
        "sign_not_found_signing_identity",
        "sign_not_found_identity",
        "sign_documents_already_exist",
        "sign_no_documents_selected",
        // Issuance
        ISSUANCE_TX_CODE_INVALID,
        ISSUANCE_TX_CODE_INACTIVE,
        ATTESTATION_CERTIFICATE_INVALID
    )

    fun extractErrorCode(throwable: Throwable?): String? {
        val message = throwable?.message
        if (isAttestationCertificateInvalid(message)) {
            return ATTESTATION_CERTIFICATE_INVALID
        }
        if (message != null && ALLOWED_ERROR_TAGS.contains(message)) {
            return message
        }
        return when {
            throwable is ApiError.NetworkException -> throwable.message ?: "network_error"
            throwable is ApiError.UnauthorizedException -> "unauthorized"
            throwable is ApiError.ServerException -> "server_error"
            throwable is ApiError.BadRequestException -> "bad_request"
            throwable is ApiError.ForbiddenException -> "forbidden"
            throwable is ApiError.ApiException -> "api_error"

            throwable is UnknownHostException -> "unknown_host"
            throwable is SocketTimeoutException -> "timeout"

            throwable?.message?.contains("UnknownHostException") == true -> "unknown_host"
            throwable?.message?.contains("SocketTimeoutException") == true -> "timeout"
            throwable?.message?.contains("ECONNRESET") == true -> "connection_reset"
            throwable?.message?.contains("SSL") == true -> "ssl_error"

            throwable is ApiError.ValidationException ->
                if (throwable.errorTag != null && ALLOWED_ERROR_TAGS.contains(throwable.errorTag)) {
                    throwable.errorTag
                } else {
                    "validation_error"
                }

            else -> null
        }
    }

    fun extractErrorCode(errorMessage: String?): String? {
        Log.d("ErrorUtils", "extractErrorCode: $errorMessage")
        if (isAttestationCertificateInvalid(errorMessage)) {
            return ATTESTATION_CERTIFICATE_INVALID
        }
        return extractErrorCode(Throwable(errorMessage))
    }

    private fun isAttestationCertificateInvalid(message: String?): Boolean {
        if (message.isNullOrBlank()) {
            return false
        }
        val normalized = message.lowercase()
        return (
            normalized.contains("key: 'certificate'") &&
                normalized.contains("failed on the 'invalid' tag")
            ) ||
            (
                normalized.contains("certificate signed by unknown authority") &&
                    normalized.contains("ecdsa verification failure")
                )
    }
}
