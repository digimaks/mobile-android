// SPDX-License-Identifier: EUPL-1.2

/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package lv.zzdats.commonfeature.features.document_details.transformer

import eu.europa.ec.eudi.wallet.document.IssuedDocument
import lv.zzdats.businesslogic.extensions.compareLocaleLanguage
import lv.zzdats.commonfeature.features.document_details.model.DocumentDetail
import lv.zzdats.commonfeature.features.document_details.model.DocumentUi
import lv.zzdats.commonfeature.features.document_details.model.DocumentUiIssuanceState
import lv.zzdats.commonfeature.util.DocumentFieldExtractor
import lv.zzdats.commonfeature.util.DocumentFieldExtractor.extractFirstName
import lv.zzdats.commonfeature.util.DocumentFieldExtractor.extractLastName
import lv.zzdats.commonfeature.util.DocumentFieldExtractor.formatPidPersonalNumberForDisplay
import lv.zzdats.commonfeature.util.DocumentFieldExtractor.removePnolvPrefix
import lv.zzdats.commonfeature.util.DocumentJsonKeys
import lv.zzdats.commonfeature.util.convertAnyToFormattedDate
import lv.zzdats.commonfeature.util.documentHasExpired
import lv.zzdats.commonfeature.util.extractDrivingPrivileges
import lv.zzdats.commonfeature.util.extractFullNameFromDocumentOrEmpty
import lv.zzdats.commonfeature.util.extractValueFromDocumentOrEmpty
import lv.zzdats.commonfeature.util.parseKeyValueUi
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.extension.localizedDocumentMetadata
import lv.zzdats.corelogic.extension.localizedIssuerMetadata
import lv.zzdats.corelogic.model.DocumentIdentifier
import lv.zzdats.corelogic.model.toDocumentIdentifier
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.resourceslogic.R

object DocumentDetailsTransformer {

    suspend fun transformToUiItem(
        document: IssuedDocument,
        resourceProvider: ResourceProvider,
        walletCoreDocumentsController: WalletCoreDocumentsController,
    ): DocumentUi? {

        val documentIsRevoked =
            walletCoreDocumentsController.isDocumentRevoked(document.id)

        val documentIdentifierUi = document.toDocumentIdentifier()
        val userLocale = resourceProvider.getLocale()

        val detailsItems: List<DocumentDetail> = document.data.claims
            .map { claim ->
                val fallbackDisplay = when (documentIdentifierUi) {
                    DocumentIdentifier.MdocESign, DocumentIdentifier.MdocESeal ->
                        localizeEParakstsLabel(claim.identifier, resourceProvider)
                    else -> null
                }

                transformToDocumentDetail(
                    displayKey = claim.issuerMetadata?.display?.firstOrNull {
                        resourceProvider.getLocale().compareLocaleLanguage(it.locale)
                    }?.name ?: claim.issuerMetadata?.display?.firstOrNull()?.name ?: fallbackDisplay,
                    key = claim.identifier,
                    item = claim.value ?: "",
                    resourceProvider = resourceProvider
                )
            }
            .filterNot { detail ->
                detail.identifier == "type" && documentIdentifierUi in listOf(DocumentIdentifier.MdocESign, DocumentIdentifier.MdocESeal)
            }

        val documentExpirationDate = getDocumentExpiryDate(document)

        val documentHasExpired = if (documentExpirationDate != null) {
            documentHasExpired(documentExpirationDate = documentExpirationDate)
        } else {
            false
        }

        val displayNumber = DocumentFieldExtractor.extractDisplayNumber(documentIdentifierUi, document).removePnolvPrefix()
        val description = DocumentFieldExtractor.extractDescription(documentIdentifierUi, document)
        val additionalInfo = DocumentFieldExtractor.extractAdditionalInfo(documentIdentifierUi, document)
        val description1 = extractFirstName(documentIdentifierUi, document)
        val description2 = extractLastName(documentIdentifierUi, document)

        val issuingAuthority = extractValueFromDocumentOrEmpty(document, DocumentJsonKeys.ISSUING_AUTHORITY)
        val issuanceMethod = walletCoreDocumentsController.getDocumentIssuanceMethod(document.id)?.value

        val issuerDisplay = document.localizedIssuerMetadata(userLocale)
        val documentDisplay = document.localizedDocumentMetadata(userLocale)

        val documentIssuanceState = when {
            documentIsRevoked -> DocumentUiIssuanceState.Revoked
            documentHasExpired -> DocumentUiIssuanceState.Expired
            else -> DocumentUiIssuanceState.Issued
        }


        return DocumentUi(
            documentId = document.id,
            documentName = document.name,
            documentIdentifier = documentIdentifierUi,
            documentExpirationDate = documentExpirationDate,
            documentHasExpired = documentHasExpired,
            documentDetails = detailsItems,
            userFullName = extractFullNameFromDocumentOrEmpty(document),
            documentIssuanceState = documentIssuanceState,
            issuingAuthority = issuingAuthority,
            issuerCountry = getDocumentIssuerCountry(document),
            issuanceDate = getDocumentIssuanceDate(document),
            issuanceMethod = issuanceMethod,
            displayNumber = displayNumber,
            description = description,
            additionalInfo = additionalInfo,
            documentIsBookmarked = false,
            issuerDisplay = issuerDisplay,
            documentDisplay = documentDisplay,
            description1 = "$description1 $description2",
            description2 = displayNumber,
        )
    }

}

private fun localizeEParakstsLabel(key: String, rp: ResourceProvider): String? =
    when (key) {
        "sid" -> rp.getString(R.string.eparaksts_sid_label)
        "cn" -> rp.getString(R.string.eparaksts_cn_label)
        "expiresOn" -> rp.getString(R.string.eparaksts_expires_on_label)
        "issuedOn" -> rp.getString(R.string.eparaksts_issued_on_label)
        "type" -> rp.getString(R.string.eparaksts_type_label)
        else -> null
    }

fun transformToDocumentDetail(
    key: String,
    displayKey: String?,
    item: Any,
    resourceProvider: ResourceProvider
): DocumentDetail {

    val values = StringBuilder()
    val localizedKey = displayKey ?: key

    parseKeyValueUi(
        item = item,
        groupIdentifier = localizedKey,
        groupIdentifierKey = key,
        resourceProvider = resourceProvider,
        allItems = values
    )
    val groupedValues = values.toString()

    return when (key) {
        DocumentJsonKeys.SIGNATURE,
        DocumentJsonKeys.PORTRAIT -> DocumentDetail(
            identifier = key,
            title = localizedKey,
            base64Image = groupedValues
        )

        DocumentJsonKeys.DRIVING_PRIVILEGES -> DocumentDetail(
            identifier = key,
            title = localizedKey,
            value = extractDrivingPrivileges(item)
        )

        DocumentJsonKeys.PID_ID_NUMBER -> DocumentDetail(
            identifier = key,
            title = localizedKey,
            value = groupedValues.formatPidPersonalNumberForDisplay()
        )

        DocumentJsonKeys.ISSUANCE_DATE,
        DocumentJsonKeys.EXPIRY_DATE,
        DocumentJsonKeys.ISSUE_DATE,
        DocumentJsonKeys.DIPLOMA_ISSUANCE_DATE,
        DocumentJsonKeys.SIGNING_ISSUANCE_DATE,
        DocumentJsonKeys.SIGNING_EXPIRY_DATE -> DocumentDetail(
            identifier = key,
            title = localizedKey,
            value = convertAnyToFormattedDate(item) ?: groupedValues
        )

        else -> DocumentDetail(
            identifier = key,
            title = localizedKey,
            value = groupedValues
        )
    }
}

data class VehicleCategory(
    val vehicleCategoryCode: String,
    val issueDate: String,
    val expiryDate: String,
    val restrictions: List<Restriction>
)

data class Restriction(
    val sign: String,
    val value: String
)

private fun getDocumentIssuanceDate(document: IssuedDocument): String {
    val possibleKeys = listOf(
        DocumentJsonKeys.ISSUANCE_DATE,    // PID
        DocumentJsonKeys.ISSUE_DATE,   // MDL
        DocumentJsonKeys.DIPLOMA_ISSUANCE_DATE,  // Diploma,
        DocumentJsonKeys.SIGNING_ISSUANCE_DATE,
        DocumentJsonKeys.JWT_ISSUED_DATE,
    )

    val claimDate = possibleKeys.firstNotNullOfOrNull { key ->
        val value = extractValueFromDocumentOrEmpty(
            document = document,
            key = key
        )
        val formatted = convertAnyToFormattedDate(value)
        if (!formatted.isNullOrEmpty()) formatted else null
    }

    if (!claimDate.isNullOrEmpty()) return claimDate

    return convertAnyToFormattedDate(document.createdAt) ?: ""
}

private fun getDocumentExpiryDate(document: IssuedDocument): String {
    val possibleKeys = listOf(
        DocumentJsonKeys.EXPIRY_DATE,    // PID
        DocumentJsonKeys.SIGNING_EXPIRY_DATE,   // SIGNING
        DocumentJsonKeys.JWT_EXPIRTY_DATE,
        DocumentJsonKeys.SIGNING_EXPIRY_DATE
    )

    return possibleKeys.firstNotNullOfOrNull { key ->
        val value = extractValueFromDocumentOrEmpty(
            document = document,
            key = key
        )
        val formatted = convertAnyToFormattedDate(value)
        if (!formatted.isNullOrEmpty()) formatted else null
    } ?: ""
}

private fun getDocumentIssuerCountry(document: IssuedDocument): String {
    val possibleKeys = listOf(
        DocumentJsonKeys.ISSUER_COUNTRY,      // Standard
        DocumentJsonKeys.DIPLOMA_ISSUER_COUNTRY // Diploma
    )

    return possibleKeys.firstNotNullOfOrNull { key ->
        val value = extractValueFromDocumentOrEmpty(
            document = document,
            key = key
        )
        if (value.isNotEmpty()) value else null
    } ?: ""
}
