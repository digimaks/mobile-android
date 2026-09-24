// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.proximityfeature.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lv.zzdats.corelogic.controller.WalletCorePresentationController

data class ProximitySuccessData(
    val verifierName: String?,
    val verifierIsTrusted: Boolean,
    val documentsSharedCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class ProximitySuccessPartialState {
    data class Success(val data: ProximitySuccessData) : ProximitySuccessPartialState()
    data class Failure(val error: String) : ProximitySuccessPartialState()
}

interface ProximitySuccessInteractor {
    fun getSuccessData(): Flow<ProximitySuccessPartialState>
    fun stopPresentation()
}

class ProximitySuccessInteractorImpl(
    private val walletCorePresentationController: WalletCorePresentationController
) : ProximitySuccessInteractor {

    override fun getSuccessData(): Flow<ProximitySuccessPartialState> = flow {
        try {
            val successData = ProximitySuccessData(
                verifierName = walletCorePresentationController.verifierName,
                verifierIsTrusted = walletCorePresentationController.verifierIsTrusted == true,
                documentsSharedCount = walletCorePresentationController.disclosedDocuments?.size ?: 0
            )
            emit(ProximitySuccessPartialState.Success(successData))
        } catch (e: Exception) {
            emit(ProximitySuccessPartialState.Failure(e.message ?: "Unknown error"))
        }
    }

    override fun stopPresentation() {
        try {
            walletCorePresentationController.stopPresentation()
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
    }
}