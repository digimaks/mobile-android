// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.transactionsfeature

import kotlinx.coroutines.launch
import lv.zzdats.corelogic.model.DocumentIdentifier
import lv.zzdats.corelogic.model.toDocumentIdentifier
import lv.zzdats.resourceslogic.bridge.TRANSACTIONS
import lv.zzdats.transactionsfeature.ui.TransactionsInteractor
import lv.zzdats.transactionsfeature.ui.TransactionsInteractorGetTransactionsPartialState
import lv.zzdats.webbridge.core.BaseBridge
import lv.zzdats.webbridge.core.BridgeRequest
import lv.zzdats.webbridge.core.BridgeResponse

class TransactionsBridge(
    private val transactionsInteractor: TransactionsInteractor
) : BaseBridge() {

    override fun getName() = TRANSACTIONS.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when (request.function) {
            TRANSACTIONS.GET_TRANSACTIONS -> handleGetTransactions(request)
            else -> createErrorResponse(request, "UNKNOWN_FUNCTION")
        }
    }

    private fun handleGetTransactions(request: BridgeRequest): BridgeResponse {
        val documentId = (request.data as? Map<*, *>)?.get("documentId") as? String

        coroutineScope.launch {
            transactionsInteractor.getTransactions(documentId).collect { result ->
                when (result) {
                    is TransactionsInteractorGetTransactionsPartialState.Success -> {
                        val transactions = result.transactionList
                            .sortedByDescending { it.timestamp }
                            .map { transaction ->
                                val isSigningDoc = transaction.docType == DocumentIdentifier.MdocESign.formatType ||
                                        transaction.docType == DocumentIdentifier.MdocESeal.formatType

                                mapOf(
                                    "id" to transaction.id,
                                    "documentId" to transaction.documentId,
                                    "documentIdentifier" to transaction.docType.toDocumentIdentifier().formatType,
                                    "documentDisplay" to mapOf(
                                        "name" to if (isSigningDoc) "" else transaction.nameSpace
                                    ),
                                    "timestamp" to transaction.timestamp,
                                    "eventType" to transaction.eventType,
                                    "status" to transaction.status,
                                    "authority" to transaction.authority
                                )
                        }

                        emitEvent(
                            createSuccessResponse(
                                request,
                                mapOf("transactions" to transactions)
                            )
                        )
                    }
                    is TransactionsInteractorGetTransactionsPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, result.error))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }
}