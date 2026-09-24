// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.wallet

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lv.zzdats.businesslogic.controller.PrefsController
import lv.zzdats.businesslogic.controller.crypto.SecureAreaRepository
import lv.zzdats.commonfeature.util.extractFirstNameFromDocumentOrEmpty
import lv.zzdats.corelogic.controller.DeleteAllDocumentsPartialState
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.storagelogic.service.DatabaseService

interface WalletInteractor {
    fun deleteWallet(
        mainPidDocumentId: String? = null
    ): Flow<DeleteWalletPartialState>

    fun getFirstName(): String
}

sealed class DeleteWalletPartialState {
    data object Success : DeleteWalletPartialState()
    data class Failure(val error: String) : DeleteWalletPartialState()
}

class WalletInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val prefsController: PrefsController,
    private val databaseService: DatabaseService,
    private val secureAreaRepository: SecureAreaRepository
) : WalletInteractor {

    override fun getFirstName(): String {
        return walletCoreDocumentsController.getMainPidDocument()?.let { extractFirstNameFromDocumentOrEmpty(it) } ?: ""
    }

    override fun deleteWallet(mainPidDocumentId: String?): Flow<DeleteWalletPartialState> = flow {
        try {
            val pidDoc = mainPidDocumentId ?: walletCoreDocumentsController.getMainPidDocument()?.id

            if (pidDoc != null) {
                walletCoreDocumentsController.deleteAllDocuments(pidDoc)
                    .collect { state ->
                        when (state) {
                            is DeleteAllDocumentsPartialState.Success -> {
                                clearLocalWalletState()
                                emit(DeleteWalletPartialState.Success)
                            }
                            is DeleteAllDocumentsPartialState.Failure -> {
                                if (walletCoreDocumentsController.getAllDocuments().isEmpty()) {
                                    clearLocalWalletState()
                                    emit(DeleteWalletPartialState.Success)
                                } else {
                                    emit(DeleteWalletPartialState.Failure(state.errorMessage))
                                }
                            }
                        }
                    }
            } else {
                clearLocalWalletState()
                emit(DeleteWalletPartialState.Success)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(DeleteWalletPartialState.Failure(e.message ?: "Unknown error"))
        }
    }

    private suspend fun clearLocalWalletState() {
        databaseService.clearAllTables()
        secureAreaRepository.deleteWalletInstance()
        prefsController.clearAll()
    }
}
