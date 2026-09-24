// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.model

import eu.europa.ec.eudi.wallet.document.DocumentId

data class DeferredDocumentData(
    val documentId: DocumentId,
    val formatType: FormatType,
    val docName: String,
)