// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.model.wallet

import com.google.gson.annotations.SerializedName

data class SignDocumentRequest(
    val requestId: String,
    val asice: Boolean,
    val fileName: String,
    val redirectUrl: String,
    val redirectError: String,
    @SerializedName("crossDevice")
    val crossDevice: Boolean? = null,
    val esealSid: String? = null,
    val userId: String? = null
)

data class SignDocumentResponse(
    val redirectUrl: String,
    val sessions: List<SignSession>? = null
)

data class SignSession(
    val requestId: String,
    val sessionId: String,
)

// Identity

data class EParakstIdentitiesUrlResponse(
    val redirectUrl: String
)

data class EParakstIdentitiesResponse(
    val eSeal: List<EParakstIdentityResponse>,
    val eSign: List<EParakstIdentityResponse>
)

data class EParakstIdentityResponse(
    val Sid: String,
    val cn: String,
    val ExpiresOn: String,
    val IssuedOn: String
)

data class ValidateContainerResponse(
    val validationResponses: List<ValidationResponse>
)

data class ValidationResponse(
    val sessionId: String,
    val data: ValidationData
)

data class ValidationData(
    val includedFiles: List<IncludedFile>,
    val signatureForm: String,
    val signaturesCount: Int,
    val signaturesExt: List<SignatureExt>,
    val validSignaturesCount: Int,
    val validatedDocument: ValidatedDocument,
    val validationLevel: String
)

data class IncludedFile(
    val filename: String
)

data class SignatureExt(
    val id: String,
    val signedBy: String,
    val signerSerialNumber: String,
    val signatureLevel: String,
    val info: SignatureInfo,
)

data class SignatureInfo(
    val bestSignatureTime: String
)

data class ValidatedDocument(
    val filename: String
)

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Int,
    @SerializedName("scope")
    val scope: String
)
