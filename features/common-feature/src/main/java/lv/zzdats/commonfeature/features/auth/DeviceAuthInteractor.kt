// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.auth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import lv.zzdats.authlogic.controller.auth.BiometricsAvailability
import lv.zzdats.authlogic.controller.auth.DeviceAuthController
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationPolicy
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationResult
import lv.zzdats.authlogic.model.BiometricCrypto
import lv.zzdats.authlogic.service.AuthService
import lv.zzdats.corelogic.controller.IssuanceMethod
import lv.zzdats.corelogic.controller.IssueDocumentPartialState
import lv.zzdats.corelogic.controller.IssueDocumentsPartialState
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.networklogic.api.wallet.DocumentType
import lv.zzdats.networklogic.api.wallet.WalletApiClient
import lv.zzdats.networklogic.error.ApiError.UnauthorizedException
import lv.zzdats.resourceslogic.provider.ResourceProvider

interface DeviceAuthenticationInteractor {
    fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit)
    fun authenticateWithBiometrics(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult,
        policy: DeviceAuthenticationPolicy = DeviceAuthenticationPolicy.DEFAULT
    )

    fun launchBiometricSystemScreen()
    fun issueDocumentByType(
        documentType: DocumentType,
        issuanceMethod: IssuanceMethod
    ): Flow<IssueDocumentPartialState>
}

class DeviceAuthenticationInteractorImpl(
    private val deviceAuthenticationController: DeviceAuthController,
    private val walletApiClient: WalletApiClient,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val authService: AuthService,
    private val resourceProvider: ResourceProvider
) : DeviceAuthenticationInteractor {

    override fun launchBiometricSystemScreen() {
        deviceAuthenticationController.launchBiometricSystemScreen()
    }

    override fun getBiometricsAvailability(listener: (BiometricsAvailability) -> Unit) {
        deviceAuthenticationController.deviceSupportsBiometrics(listener)
    }

    override fun authenticateWithBiometrics(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult,
        policy: DeviceAuthenticationPolicy
    ) {
        deviceAuthenticationController.authenticate(
            context,
            crypto,
            notifyOnAuthenticationFailure,
            resultHandler,
            policy
        )
    }

    var savedDocType: DocumentType? = null

    override fun issueDocumentByType(
        documentType: DocumentType,
        issuanceMethod: IssuanceMethod
    ): Flow<IssueDocumentPartialState> = flow {
        try {
            if(savedDocType == null)
                savedDocType = documentType
            val offer = walletApiClient.getDocumentOffer(savedDocType!!).fold(
                onSuccess = {
                    Log.d("offer","offer ${it.offerUrl}")
                    savedDocType = null
                    it
                            },
                onFailure = { error ->
                    if (error is UnauthorizedException) {
                        coroutineScope {
                            launch {
                                resourceProvider.provideContext().let { context ->
                                    val method = EparakstsAuthLauncher.preferredAuthMethod(context)
                                    val url = authService.buildAuthUrl(method)
                                    EparakstsAuthLauncher.launch(context, url.url)
                                }
                            }
                        }

                        emit(IssueDocumentPartialState.Failure("Authentication required"))
                        return@flow
                    }
                    throw error
                }
            )

            // Continue with normal document issuance flow
            walletCoreDocumentsController.issueDocumentsByOfferUri(
                offerUri = offer.offerUrl,
                txCode = null,
                issuanceMethod = issuanceMethod
            ).collect { result ->
                when (result) {
                    is IssueDocumentsPartialState.Success -> {
                        emit(IssueDocumentPartialState.Success(result.documentIds.first()))
                    }
                    is IssueDocumentsPartialState.Failure -> {
                        emit(
                            IssueDocumentPartialState.Failure(
                                errorMessage = result.errorMessage,
                                cause = result.cause
                            )
                        )
                    }
                    is IssueDocumentsPartialState.UserAuthRequired -> {
                        emit(IssueDocumentPartialState.UserAuthRequired(
                            result.crypto,
                            result.resultHandler
                        ))
                    }
                    else -> {
                        emit(IssueDocumentPartialState.Failure(resourceProvider.genericErrorMessage()))
                    }
                }
            }
        } catch (e: Exception) {
            emit(
                IssueDocumentPartialState.Failure(
                    errorMessage = e.message ?: resourceProvider.genericErrorMessage(),
                    cause = e
                )
            )
        }
    }
}
