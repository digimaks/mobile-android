// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.transactionsfeature.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lv.zzdats.businesslogic.extensions.safeAsync
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.storagelogic.dao.TransactionLogDao
import lv.zzdats.storagelogic.model.TransactionLog

sealed class TransactionsInteractorGetTransactionsPartialState {
    data class Success(
        val transactionList: List<TransactionLog>,
    ) : TransactionsInteractorGetTransactionsPartialState()

    data class Failure(val error: String) : TransactionsInteractorGetTransactionsPartialState()
}

interface TransactionsInteractor {
    fun getTransactions(documentId: String?): Flow<TransactionsInteractorGetTransactionsPartialState>
}

class TransactionsInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val transactionLogDao: TransactionLogDao,
) : TransactionsInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()


    override fun getTransactions(documentId: String?): Flow<TransactionsInteractorGetTransactionsPartialState> = flow {
        val transactions = if (documentId == null) {
            transactionLogDao.retrieveAll()
        } else {
            transactionLogDao.retrieveByDocumentId(documentId)
        }

        emit(
            TransactionsInteractorGetTransactionsPartialState.Success(
                transactionList = transactions,
            )
        )
    }.safeAsync {
        TransactionsInteractorGetTransactionsPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }
}