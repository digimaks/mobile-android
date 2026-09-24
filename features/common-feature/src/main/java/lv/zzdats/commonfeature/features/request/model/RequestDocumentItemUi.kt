// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.request.model

import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.format.DocumentFormat
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import lv.zzdats.corelogic.model.DomainClaim

sealed class DomainDocumentFormat {
    data object SdJwtVc : DomainDocumentFormat()
    data class MsoMdoc(val namespace: String) :
        DomainDocumentFormat()

    companion object {
        fun getFormat(format: DocumentFormat, namespace: String?): DomainDocumentFormat {
            return when (format) {
                is SdJwtVcFormat -> SdJwtVc
                is MsoMdocFormat -> MsoMdoc(
                    namespace = namespace.toString()
                )
            }
        }
    }
}

data class DocumentPayloadDomain(
    val docName: String,
    val docId: DocumentId,
    val domainDocFormat: DomainDocumentFormat,
    val docClaimsDomain: List<DomainClaim>,
)