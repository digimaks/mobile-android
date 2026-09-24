// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.document_details.model

import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.metadata.IssuerMetadata
import lv.zzdats.corelogic.model.DocumentIdentifier

enum class DocumentUiIssuanceState {
    Issued, Pending, Failed, Expired, Revoked
}

data class DocumentUi(
    val documentId: DocumentId,
    val documentName: String,
    val documentIdentifier: DocumentIdentifier,
    val documentIssuanceState: DocumentUiIssuanceState,
    val description1: String? = null,
    val description2: String? = null,
    val description3: String? = null,
    val documentExpirationDate: String,
    val documentHasExpired: Boolean,
    val documentIsBookmarked: Boolean,
    val documentDetails: List<DocumentDetail>,
    val userFullName: String? = null,
    val issuingAuthority: String? = null,
    val issuerCountry: String? = null,
    val issuanceDate: String? = null,
    val issuanceMethod: String? = null,
    val displayNumber: String? = null,
    val description: String? = null,
    val additionalInfo: String? = null,
    val issuerDisplay: IssuerMetadata.IssuerDisplay? = null,
    val documentDisplay: IssuerMetadata.Display? = null
)
