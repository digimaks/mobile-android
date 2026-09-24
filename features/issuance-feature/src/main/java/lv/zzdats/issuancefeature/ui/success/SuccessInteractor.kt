// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.issuancefeature.ui.success

import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lv.zzdats.businesslogic.extensions.safeAsync
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.commonfeature.util.extractFullNameFromDocumentOrEmpty
import lv.zzdats.resourceslogic.provider.ResourceProvider

sealed class SuccessFetchDocumentByIdPartialState {
    data class Success(
        val document: IssuedDocument,
        val documentName: String,
        val fullName: String
    ) : SuccessFetchDocumentByIdPartialState()

    data class Failure(val error: String) : SuccessFetchDocumentByIdPartialState()
}

interface SuccessInteractor {
    fun fetchDocumentById(documentId: DocumentId): Flow<SuccessFetchDocumentByIdPartialState>
    fun fetchMainPidDocument(): Flow<SuccessFetchDocumentByIdPartialState>
}

class SuccessInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController
) : SuccessInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun fetchDocumentById(documentId: DocumentId): Flow<SuccessFetchDocumentByIdPartialState> =
        flow {
            val document = walletCoreDocumentsController.getDocumentById(documentId = documentId)
                    as? IssuedDocument
            document?.let { issuedDocument ->
                emit(
                    SuccessFetchDocumentByIdPartialState.Success(
                        document = issuedDocument,
                        documentName = issuedDocument.name,
                        fullName = extractFullNameFromDocumentOrEmpty(issuedDocument)
                    )
                )
            } ?: emit(SuccessFetchDocumentByIdPartialState.Failure(genericErrorMsg))
        }.safeAsync {
            SuccessFetchDocumentByIdPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun fetchMainPidDocument(): Flow<SuccessFetchDocumentByIdPartialState> =
        flow {
            val document = walletCoreDocumentsController.getMainPidDocument()
                    as? IssuedDocument
            document?.let { issuedDocument ->
                emit(
                    SuccessFetchDocumentByIdPartialState.Success(
                        document = issuedDocument,
                        documentName = issuedDocument.name,
                        fullName = extractFullNameFromDocumentOrEmpty(issuedDocument)
                    )
                )
            } ?: emit(SuccessFetchDocumentByIdPartialState.Failure(genericErrorMsg))
        }.safeAsync {
            SuccessFetchDocumentByIdPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }
}