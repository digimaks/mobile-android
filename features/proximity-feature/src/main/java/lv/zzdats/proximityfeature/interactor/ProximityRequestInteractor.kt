// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.proximityfeature.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import lv.zzdats.businesslogic.extensions.safeAsync
import lv.zzdats.businesslogic.provider.UuidProvider
import lv.zzdats.commonfeature.config.RequestUriConfig
import lv.zzdats.commonfeature.config.toDomainConfig
import lv.zzdats.commonfeature.features.request.transformer.PresentationDocument
import lv.zzdats.commonfeature.features.request.transformer.RequestTransformer
import lv.zzdats.corelogic.controller.TransferEventPartialState
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.controller.WalletCorePresentationController
import lv.zzdats.resourceslogic.provider.ResourceProvider

sealed class ProximityRequestPartialState {
    data class Success(
        val verifierName: String? = null,
        val verifierIsTrusted: Boolean,
        val requestDocuments: List<PresentationDocument>,
        val requestPurpose: String? = null
    ) : ProximityRequestPartialState()

    data class NoData(
        val verifierName: String? = null,
        val verifierIsTrusted: Boolean,
    ) : ProximityRequestPartialState()

    data class Failure(val error: String) : ProximityRequestPartialState()
    data object Disconnect : ProximityRequestPartialState()
    data object Loading : ProximityRequestPartialState()
}

interface ProximityRequestInteractor {
    fun getRequestDocuments(): Flow<ProximityRequestPartialState>
    fun stopPresentation()
    fun updateRequestedDocuments(items: List<PresentationDocument>)
    fun setConfig(config: RequestUriConfig)
    fun cleanup()
}

class ProximityRequestInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val uuidProvider: UuidProvider,
    private val walletCorePresentationController: WalletCorePresentationController,
    private val walletCoreDocumentsController: WalletCoreDocumentsController
) : ProximityRequestInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun setConfig(config: RequestUriConfig) {
        walletCorePresentationController.setConfig(config.toDomainConfig())
    }

    override fun getRequestDocuments(): Flow<ProximityRequestPartialState> =
        walletCorePresentationController.events.mapNotNull { response ->
            when (response) {
                is TransferEventPartialState.RequestReceived -> {
                    if (response.requestData.all { it.requestedItems.isEmpty() }) {
                        ProximityRequestPartialState.NoData(
                            verifierName = response.verifierName,
                            verifierIsTrusted = response.verifierIsTrusted,
                        )
                    } else {
                        val documentsDomain = RequestTransformer.transformToDomainItems(
                            storageDocuments = walletCoreDocumentsController.getAllIssuedDocuments(),
                            requestDocuments = response.requestData,
                            resourceProvider = resourceProvider,
                            uuidProvider = uuidProvider
                        ).getOrThrow()
                            .filterNot {
                                walletCoreDocumentsController.isDocumentRevoked(it.docId)
                            }

                        if (documentsDomain.isNotEmpty()) {
                            ProximityRequestPartialState.Success(
                                verifierName = response.verifierName,
                                verifierIsTrusted = response.verifierIsTrusted,
                                requestDocuments = RequestTransformer.transformToPresentationItems(
                                    documentsDomain = documentsDomain,
                                    resourceProvider = resourceProvider,
                                ),
                            )
                        } else {
                            ProximityRequestPartialState.NoData(
                                verifierName = response.verifierName,
                                verifierIsTrusted = response.verifierIsTrusted,
                            )
                        }
                    }
                }

                is TransferEventPartialState.Error -> {
                    ProximityRequestPartialState.Failure(error = response.error)
                }

                is TransferEventPartialState.Disconnected -> {
                    ProximityRequestPartialState.Disconnect
                }

                else -> null
            }
        }.safeAsync {
            ProximityRequestPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun stopPresentation() {
        walletCorePresentationController.stopPresentation()
    }

    override fun updateRequestedDocuments(items: List<PresentationDocument>) {
        val disclosedDocuments = RequestTransformer.createDisclosedDocuments(items)
        walletCorePresentationController.updateRequestedDocuments(disclosedDocuments.toMutableList())
    }

    override fun cleanup() {
        try {
            walletCorePresentationController.stopPresentation()
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
    }
}