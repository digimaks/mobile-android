// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.presentationfeature.interactor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import lv.zzdats.businesslogic.extensions.safeAsync
import lv.zzdats.businesslogic.provider.UuidProvider
import lv.zzdats.commonfeature.config.RequestUriConfig
import lv.zzdats.commonfeature.config.toDomainConfig
import lv.zzdats.commonfeature.features.PresentationConfigStore
import lv.zzdats.commonfeature.features.request.transformer.PresentationDocument
import lv.zzdats.commonfeature.features.request.transformer.RequestTransformer
import lv.zzdats.corelogic.controller.TransferEventPartialState
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.controller.WalletCorePresentationController
import lv.zzdats.resourceslogic.provider.ResourceProvider

sealed class PresentationRequestInteractorPartialState {
    data class Success(
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
        val requestDocuments: List<PresentationDocument>,
    ) : PresentationRequestInteractorPartialState()

    data class NoData(
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
    ) : PresentationRequestInteractorPartialState()

    data class Failure(val error: String) : PresentationRequestInteractorPartialState()
    data object Disconnect : PresentationRequestInteractorPartialState()
}

interface PresentationRequestInteractor {
    fun getRequestDocuments(): Flow<PresentationRequestInteractorPartialState>
    fun stopPresentation()
    fun updateRequestedDocuments(items: List<PresentationDocument>)
    fun setConfig(config: RequestUriConfig)
}

class PresentationRequestInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val uuidProvider: UuidProvider,
    private val walletCorePresentationController: WalletCorePresentationController,
    private val walletCoreDocumentsController: WalletCoreDocumentsController
) : PresentationRequestInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun setConfig(config: RequestUriConfig) {
        walletCorePresentationController.setConfig(config.toDomainConfig())
    }

    override fun getRequestDocuments(): Flow<PresentationRequestInteractorPartialState> =
        walletCorePresentationController.events.mapNotNull { response ->
            when (response) {
                is TransferEventPartialState.RequestReceived -> {
                    if (response.requestData.all { it.requestedItems.isEmpty() }) {
                        PresentationRequestInteractorPartialState.NoData(
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

                        if (documentsDomain.isNotEmpty()) {
                            PresentationRequestInteractorPartialState.Success(
                                verifierName = response.verifierName,
                                verifierIsTrusted = response.verifierIsTrusted,
                                requestDocuments = RequestTransformer.transformToPresentationItems(
                                    documentsDomain = documentsDomain,
                                    resourceProvider = resourceProvider,
                                ),
                            )
                        } else {
                            PresentationRequestInteractorPartialState.NoData(
                                verifierName = response.verifierName,
                                verifierIsTrusted = response.verifierIsTrusted,
                            )
                        }
                    }
                }

                is TransferEventPartialState.Error -> {
                    PresentationRequestInteractorPartialState.Failure(error = response.error)
                }

                is TransferEventPartialState.Disconnected -> {
                    PresentationRequestInteractorPartialState.Disconnect
                }

                else -> null
            }
        }.safeAsync {
            PresentationRequestInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun stopPresentation() {
        PresentationConfigStore.clear()
        walletCorePresentationController.stopPresentation()
    }

    override fun updateRequestedDocuments(items: List<PresentationDocument>) {
        val disclosedDocuments = RequestTransformer.createDisclosedDocuments(items)
        walletCorePresentationController.updateRequestedDocuments(disclosedDocuments.toMutableList())
    }
}