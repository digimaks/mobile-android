// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.model

import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentedDocument
import java.time.LocalDate
import java.time.LocalDateTime
import lv.zzdats.corelogic.extension.getLocalizedDocumentName
import lv.zzdats.resourceslogic.provider.ResourceProvider
import java.util.Locale

sealed interface TransactionLogDataDomain {

    val id: String
    val name: String
    val status: TransactionLog.Status
    val creationLocalDateTime: LocalDateTime
    val creationLocalDate: LocalDate

    data class PresentationLog(
        override val id: String,
        override val name: String,
        override val status: TransactionLog.Status,
        override val creationLocalDateTime: LocalDateTime,
        override val creationLocalDate: LocalDate,
        val relyingParty: TransactionLog.RelyingParty,
        val documents: List<PresentedDocument>,
    ) : TransactionLogDataDomain

    data class IssuanceLog(
        override val id: String,
        override val name: String,
        override val status: TransactionLog.Status,
        override val creationLocalDateTime: LocalDateTime,
        override val creationLocalDate: LocalDate,
    ) : TransactionLogDataDomain

    data class SigningLog(
        override val id: String,
        override val name: String,
        override val status: TransactionLog.Status,
        override val creationLocalDateTime: LocalDateTime,
        override val creationLocalDate: LocalDate,
    ) : TransactionLogDataDomain

    companion object {
        fun TransactionLogDataDomain.getTransactionTypeLabel(resourceProvider: ResourceProvider): String {
            return when (this) {
                is PresentationLog -> "presentation"
                is IssuanceLog -> "issuance"
                is SigningLog -> "signing"
            }
        }

        fun TransactionLogDataDomain.getTransactionDocumentNames(userLocale: Locale): List<String> {
            return when (this) {
                is IssuanceLog -> {
                    //TODO change this once Core supports more transaction types
                    emptyList()
                }

                is PresentationLog -> {
                    this.documents.mapNotNull { document ->
                        document.metadata.getLocalizedDocumentName(
                            userLocale = userLocale,
                            fallback = ""
                        ).takeIf { it.isNotBlank() }
                    }.flatMap { documentName ->
                        listOf(
                            documentName,
                            documentName.replace(regex = "\\s".toRegex(), replacement = "")
                        )
                    }
                }

                is SigningLog -> {
                    //TODO change this once Core supports more transaction types
                    emptyList()
                }
            }
        }
    }
}