// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.util

import android.util.Log
import lv.zzdats.corelogic.model.DocumentIdentifier
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import lv.zzdats.commonfeature.features.document_details.model.DocumentUi
import lv.zzdats.corelogic.extension.localizedIssuerMetadata
import lv.zzdats.resourceslogic.provider.ResourceProvider
import androidx.core.net.toUri

object DocumentFieldExtractor {
    private val uriCacheManager = UriCacheManager(24 * 60 * 60 * 1000L) // 1 day cache
    fun extractDisplayNumber(documentIdentifier: DocumentIdentifier, document: IssuedDocument): String =
        when (documentIdentifier) {
            DocumentIdentifier.MdocPid, DocumentIdentifier.SdJwtPid ->
                extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.PID_ID_NUMBER)
                    .formatPidPersonalNumberForDisplay()
            DocumentIdentifier.MdocMDL, DocumentIdentifier.SdJwtMDL ->
                extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.MDL_ID_NUMBER)
            DocumentIdentifier.MdocRTUDiploma ->
                extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.DIPLOMA_ID_NUMBER)
            else -> ""
        }

    fun extractDescription(documentIdentifier: DocumentIdentifier, document: IssuedDocument): String =
        when (documentIdentifier) {
            DocumentIdentifier.MdocPid, DocumentIdentifier.SdJwtPid ->
                extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.ISSUER_COUNTRY)
            DocumentIdentifier.MdocRTUDiploma -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.DIPLOMA_THEMATIC_AREA)
            DocumentIdentifier.MdocMDL, DocumentIdentifier.SdJwtMDL -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.ISSUER_COUNTRY)
            DocumentIdentifier.MdocESeal -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.SIGNING_NAME)
            else -> ""
        }

    fun extractFirstName(documentIdentifier: DocumentIdentifier, document: IssuedDocument): String =
        when (documentIdentifier) {
            DocumentIdentifier.MdocPid, DocumentIdentifier.SdJwtPid -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.FIRST_NAME)
            DocumentIdentifier.MdocMDL, DocumentIdentifier.SdJwtMDL -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.FIRST_NAME)
            DocumentIdentifier.MdocESign, DocumentIdentifier.MdocESeal -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.SIGNING_NAME)
            else -> ""
        }

    fun extractLastName(documentIdentifier: DocumentIdentifier, document: IssuedDocument): String =
        when (documentIdentifier) {
            DocumentIdentifier.MdocPid, DocumentIdentifier.SdJwtPid -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.LAST_NAME)
            DocumentIdentifier.MdocMDL, DocumentIdentifier.SdJwtMDL -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.LAST_NAME)
            else -> ""
        }

    fun extractAdditionalInfo(documentIdentifier: DocumentIdentifier, document: IssuedDocument): String =
        when (documentIdentifier) {
            DocumentIdentifier.MdocPid, DocumentIdentifier.SdJwtPid -> ""
            DocumentIdentifier.MdocRTUDiploma -> extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.DIPLOMA_ACHIEVEMENT)
            DocumentIdentifier.MdocMDL, DocumentIdentifier.SdJwtMDL -> {
                val privileges = document.data.claims
                    .firstOrNull { it.identifier == DocumentJsonKeys.DRIVING_PRIVILEGES }
                    ?.value
                extractDrivingPrivileges(privileges)
            }
            else -> ""
        }

    fun String.removePnolvPrefix(): String =
        if (this.startsWith("PNOLV")) this.removePrefix("PNOLV-") else this

    fun String.formatPidPersonalNumberForDisplay(): String {
        val normalized = removePnolvPrefix().trim()
        val digitsOnly = normalized.filter { it.isDigit() }

        return if (digitsOnly.length == 11) {
            "${digitsOnly.substring(0, 6)}-${digitsOnly.substring(6)}"
        } else {
            normalized
        }
    }

    fun DocumentUi.toWebMeta(
        resourceProvider: ResourceProvider,
        issuedDocument: IssuedDocument? = null,
        isFavorite: Boolean? = null,
    ): Map<String, Any?> {
        val userLocale = resourceProvider.getLocale()

        val resolvedIssuerDisplay = issuerDisplay

        val issuerLogoUri = resolvedIssuerDisplay?.logo?.uri

        val cachedIssuerLogoUri = issuerLogoUri.let { originalUri ->
            val cacheKey = "issuer_logo_${documentId}_${originalUri.hashCode()}"
            uriCacheManager.refetchIfExpired(cacheKey) {
                originalUri.toString().toUri()
            }.toString()
        }

        val cachedBackgroundImageUri = documentDisplay?.backgroundImageUri?.let { originalUri ->
            val cacheKey = "doc_bg_${documentId}_${originalUri.hashCode()}"
            uriCacheManager.refetchIfExpired(cacheKey) {
                originalUri.toString().toUri()
            }.toString()
        }

        val cachedDocumentLogoUri = documentDisplay?.logo?.uri?.let { originalUri ->
            val cacheKey = "doc_logo_${documentId}_${originalUri.hashCode()}"
            uriCacheManager.refetchIfExpired(cacheKey) {
                originalUri.toString().toUri()
            }.toString()
        }

        val backgroundColor = when (this.documentIdentifier) {
            DocumentIdentifier.MdocESign -> "#009AC9"
            DocumentIdentifier.MdocESeal -> "#DC4F0B"
            else -> documentDisplay?.backgroundColor
        }

        return mapOf(
            "id" to documentId,
            "documentIdentifier" to documentIdentifier.formatType,
            "hasExpired" to documentHasExpired,
            "status" to documentIssuanceState.name,
            "isFavorite" to documentIsBookmarked,
            "expirationDate" to documentExpirationDate,
            "issuanceDate" to issuanceDate,
            "issuanceMethod" to issuanceMethod,
            "issuerDisplay" to mapOf(
                "name" to issuerDisplay?.name,
                "issuingCountry" to issuerCountry,
                "issuingAuthority" to issuingAuthority,
                "logo" to mapOf(
                    "uri" to cachedIssuerLogoUri,
                    "altText" to issuerDisplay?.logo?.alternativeText
                )
            ),
            "documentDisplay" to mapOf(
                "name" to documentDisplay?.name,
                "backgroundColor" to backgroundColor,
                "textColor" to documentDisplay?.textColor,
                "backgroundImageUri" to cachedBackgroundImageUri,
                "description1" to description1,
                "description2" to description2,
                "description3" to description3,
                "logo" to mapOf(
                    "uri" to cachedDocumentLogoUri,
                    "altText" to documentDisplay?.logo?.alternativeText
                )
            ),
        )
    }
}
