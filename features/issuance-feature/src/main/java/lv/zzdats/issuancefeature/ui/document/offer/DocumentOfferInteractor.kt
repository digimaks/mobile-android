// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.issuancefeature.ui.document.offer

import android.content.Context
import android.util.Log
import eu.europa.ec.eudi.openid4vci.TxCodeInputMode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import lv.zzdats.authlogic.controller.auth.BiometricsAvailability
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationResult
import lv.zzdats.authlogic.model.BiometricCrypto
import lv.zzdats.authlogic.service.AuthMethod
import lv.zzdats.authlogic.service.AuthService
import lv.zzdats.businesslogic.extensions.safeAsync
import lv.zzdats.businesslogic.util.safeLet
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.commonfeature.features.user.onboarding.OnboardingInteractor
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.zzdats.commonfeature.features.auth.EparakstsAuthLauncher
import lv.zzdats.corelogic.controller.IssuanceMethod
import lv.zzdats.corelogic.controller.IssueDocumentsPartialState
import lv.zzdats.corelogic.controller.ResolveDocumentOfferPartialState
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.extension.documentIdentifier
import lv.zzdats.corelogic.extension.getIssuerName
import lv.zzdats.corelogic.extension.getName
import lv.zzdats.corelogic.model.DocumentIdentifier
import lv.zzdats.commonfeature.features.request.model.DocumentItemUi
import lv.zzdats.networklogic.api.wallet.DocumentType
import lv.zzdats.networklogic.api.wallet.WalletApiClient
import lv.zzdats.networklogic.error.ApiError.UnauthorizedException
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.uilogic.config.ConfigNavigation
import lv.zzdats.uilogic.serializer.UiSerializer
import lv.zzdats.resourceslogic.R
import lv.zzdats.corelogic.security.SecureAreaController
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import kotlin.coroutines.resume

sealed class ResolveDocumentOfferInteractorPartialState {
    data class Success(
        val documents: List<DocumentItemUi>,
        val issuerName: String,
        val txCodeLength: Int?
    ) : ResolveDocumentOfferInteractorPartialState()

    data class NoDocument(val issuerName: String) : ResolveDocumentOfferInteractorPartialState()
    data class Failure(val errorMessage: String) : ResolveDocumentOfferInteractorPartialState()
}

sealed class IssueDocumentsInteractorPartialState {
    class Success() : IssueDocumentsInteractorPartialState()

    class DeferredSuccess() : IssueDocumentsInteractorPartialState()

    data class Failure(
        val errorMessage: String,
        val errorCode: String? = null,
        val cause: Throwable? = null
    ) : IssueDocumentsInteractorPartialState()

    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult
    ) : IssueDocumentsInteractorPartialState()
}

interface DocumentOfferInteractor {
    fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferInteractorPartialState>

    fun issueDocuments(
        offerUri: String,
        issuerName: String,
        navigation: ConfigNavigation,
        txCode: String? = null,
        activity: FragmentActivity? = null
    ): Flow<IssueDocumentsInteractorPartialState>

    fun issueDocumentByType(
        documentType: DocumentType,
        activity: FragmentActivity? = null
    ): Flow<IssueDocumentsInteractorPartialState>

    fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    )

    fun resumeOpenId4VciWithAuthorization(uri: String)
}

class DocumentOfferInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    private val authService: AuthService,
    private val walletApiClient: WalletApiClient,
    private val secureAreaController: SecureAreaController,
    private val onboardingInteractor: OnboardingInteractor,
    private val logController: LogController
) : DocumentOfferInteractor {

    private companion object {
        const val TAG = "DocumentOfferInteractor"
    }

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferInteractorPartialState> =
        flow {
            walletCoreDocumentsController.resolveDocumentOffer(
                offerUri = offerUri
            ).map { response ->
                when (response) {
                    is ResolveDocumentOfferPartialState.Failure -> {
                        ResolveDocumentOfferInteractorPartialState.Failure(errorMessage = response.errorMessage)
                    }

                    is ResolveDocumentOfferPartialState.Success -> {
                        val offerHasNoDocuments = response.offer.offeredDocuments.isEmpty()
                        if (offerHasNoDocuments) {
                            ResolveDocumentOfferInteractorPartialState.NoDocument(
                                issuerName = response.offer.getIssuerName(
                                    resourceProvider.getLocale()
                                )
                            )
                        } else {

                            val codeMinLength = 4
                            val codeMaxLength = 6

                            safeLet(
                                response.offer.txCodeSpec?.inputMode,
                                response.offer.txCodeSpec?.length
                            ) { inputMode, length ->

                                if ((length !in codeMinLength..codeMaxLength) || inputMode == TxCodeInputMode.TEXT) {
                                    return@map ResolveDocumentOfferInteractorPartialState.Failure(
                                        errorMessage = resourceProvider.getString(
                                            R.string.issuance_document_offer_error_invalid_txcode_format,
                                            codeMinLength,
                                            codeMaxLength
                                        )
                                    )
                                }
                            }

                            val hasMainPid =
                                walletCoreDocumentsController.getMainPidDocument() != null

                            val hasPidInOffer =
                                response.offer.offeredDocuments.any { offeredDocument ->
                                    val id = offeredDocument.documentIdentifier
                                    id == DocumentIdentifier.MdocPid || id == DocumentIdentifier.SdJwtPid
                                }

                            if (hasMainPid || hasPidInOffer) {

                                ResolveDocumentOfferInteractorPartialState.Success(
                                    documents = response.offer.offeredDocuments.map { offeredDocument ->
                                        DocumentItemUi(
                                            title = offeredDocument.getName(
                                                resourceProvider.getLocale()
                                            ).orEmpty()
                                        )
                                    },
                                    issuerName = response.offer.getIssuerName(resourceProvider.getLocale()),
                                    txCodeLength = response.offer.txCodeSpec?.length
                                )
                            } else {
                                ResolveDocumentOfferInteractorPartialState.Failure(
                                    errorMessage = resourceProvider.getString(
                                        R.string.issuance_document_offer_error_missing_pid_text
                                    )
                                )
                            }
                        }
                    }
                }
            }.collect {
                emit(it)
            }
        }.safeAsync {
            logController.e(TAG, it)
            ResolveDocumentOfferInteractorPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun issueDocuments(
        offerUri: String,
        issuerName: String,
        navigation: ConfigNavigation,
        txCode: String?,
        activity: FragmentActivity?
    ): Flow<IssueDocumentsInteractorPartialState> =
        flow {
            ensureWalletInstanceRegistered(activity).getOrElse {
                emit(
                    IssueDocumentsInteractorPartialState.Failure(
                        errorMessage = it.localizedMessage ?: genericErrorMsg,
                        cause = it
                    )
                )
                return@flow
            }
            ensureHardwareKeyAuthenticated(activity).getOrElse {
                emit(
                    IssueDocumentsInteractorPartialState.Failure(
                        errorMessage = it.localizedMessage ?: genericErrorMsg,
                        cause = it
                    )
                )
                return@flow
            }
            walletCoreDocumentsController.issueDocumentsByOfferUri(
                offerUri = offerUri,
                txCode = txCode,
                issuanceMethod = IssuanceMethod.QR
            ).map { response ->
                when (response) {
                    is IssueDocumentsPartialState.Failure -> {
                        IssueDocumentsInteractorPartialState.Failure(
                            errorMessage = response.errorMessage,
                            errorCode = response.errorCode,
                            cause = response.cause
                        )
                    }

                    is IssueDocumentsPartialState.PartialSuccess -> {

                        val nonIssuedDocsNames: String =
                            response.nonIssuedDocuments.entries.map { it.value }.joinToString(
                                separator = ", ",
                                transform = {
                                    it
                                }
                            )

                        IssueDocumentsInteractorPartialState.Success()
                    }

                    is IssueDocumentsPartialState.Success -> {
                        IssueDocumentsInteractorPartialState.Success()
                    }

                    is IssueDocumentsPartialState.UserAuthRequired -> {
                        IssueDocumentsInteractorPartialState.UserAuthRequired(
                            crypto = response.crypto,
                            resultHandler = response.resultHandler
                        )
                    }

                    is IssueDocumentsPartialState.DeferredSuccess -> {
                        IssueDocumentsInteractorPartialState.DeferredSuccess()
                    }
                }
            }.collect {
                emit(it)
            }
        }.safeAsync {
            logController.e(TAG, it)
            IssueDocumentsInteractorPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg,
                cause = it
            )
        }

    override fun issueDocumentByType(
        documentType: DocumentType,
        activity: FragmentActivity?
    ): Flow<IssueDocumentsInteractorPartialState> = flow {
        try {
            ensureWalletInstanceRegistered(activity).getOrElse {
                emit(
                    IssueDocumentsInteractorPartialState.Failure(
                        errorMessage = it.localizedMessage ?: genericErrorMsg,
                        cause = it
                    )
                )
                return@flow
            }
            ensureHardwareKeyAuthenticated(activity).getOrElse {
                emit(
                    IssueDocumentsInteractorPartialState.Failure(
                        errorMessage = it.localizedMessage ?: genericErrorMsg,
                        cause = it
                    )
                )
                return@flow
            }
            val offer = walletApiClient.getDocumentOffer(documentType).fold(
                onSuccess = {
                    Log.d("offer","offer ${it.offerUrl}")

                    it
                },
                onFailure = { error ->
                    if (error is UnauthorizedException) {
                        logController.e(TAG) { "Issue by type requires re-authentication for ${documentType.type}" }
                        coroutineScope {
                            launch {
                                resourceProvider.provideContext()?.let { context ->
                                    val method = EparakstsAuthLauncher.preferredAuthMethod(context)
                                    val url = authService.buildAuthUrl(method)
                                    EparakstsAuthLauncher.launch(context, url.url)
                                }
                            }
                        }
                        return@flow
                    }
                    logController.e(TAG, error)
                    throw error
                }
            )

            walletCoreDocumentsController.issueDocumentsByOfferUri(
                offerUri = offer.offerUrl,
                txCode = null,
                issuanceMethod = IssuanceMethod.QR
            ).map { response ->
                when (response) {
                    is IssueDocumentsPartialState.Failure -> {
                        IssueDocumentsInteractorPartialState.Failure(
                            errorMessage = response.errorMessage,
                            errorCode = response.errorCode,
                            cause = response.cause
                        )
                    }

                    is IssueDocumentsPartialState.PartialSuccess -> {

                        val nonIssuedDocsNames: String =
                            response.nonIssuedDocuments.entries.map { it.value }.joinToString(
                                separator = ", ",
                                transform = {
                                    it
                                }
                            )

                        IssueDocumentsInteractorPartialState.Success()
                    }

                    is IssueDocumentsPartialState.Success -> {
                        IssueDocumentsInteractorPartialState.Success()
                    }

                    is IssueDocumentsPartialState.UserAuthRequired -> {
                        IssueDocumentsInteractorPartialState.UserAuthRequired(
                            crypto = response.crypto,
                            resultHandler = response.resultHandler
                        )
                    }

                    is IssueDocumentsPartialState.DeferredSuccess -> {
                        IssueDocumentsInteractorPartialState.DeferredSuccess()
                    }
                }
            }.collect {
                emit(it)
            }


        } catch (e: Exception) {
             logController.e(TAG, e)
             emit(
                 IssueDocumentsInteractorPartialState.Failure(
                     errorMessage = e.message ?: genericErrorMsg,
                     cause = e
                 )
             )
        }
    }.safeAsync {
        logController.e(TAG, it)
        IssueDocumentsInteractorPartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMsg,
            cause = it
        )
    }

    override fun handleUserAuthentication(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    ) {
        deviceAuthenticationInteractor.getBiometricsAvailability {
            when (it) {
                is BiometricsAvailability.CanAuthenticate -> {
                    deviceAuthenticationInteractor.authenticateWithBiometrics(
                        context = context,
                        crypto = crypto,
                        notifyOnAuthenticationFailure = notifyOnAuthenticationFailure,
                        resultHandler = resultHandler
                    )
                }

                is BiometricsAvailability.NonEnrolled -> {
                    deviceAuthenticationInteractor.launchBiometricSystemScreen()
                }

                is BiometricsAvailability.Failure -> {
                    resultHandler.onAuthenticationFailure()
                }
            }
        }
    }

    override fun resumeOpenId4VciWithAuthorization(uri: String) {
        walletCoreDocumentsController.resumeOpenId4VciWithAuthorization(uri)
    }

    private suspend fun ensureWalletInstanceRegistered(activity: FragmentActivity?): Result<Unit> {
        val hostActivity = activity ?: return Result.failure(IllegalStateException("activity_missing"))
        return onboardingInteractor.registerWallet(hostActivity)
    }

    private suspend fun ensureHardwareKeyAuthenticated(activity: FragmentActivity?): Result<Unit> {
        val requiresAuth = secureAreaController.isHardwareKeyUserAuthRequired()
        if (!requiresAuth) return Result.success(Unit)
        if (secureAreaController.isHardwareKeyCurrentlyAuthenticated()) {
            return Result.success(Unit)
        }
        val hostActivity = activity ?: return Result.failure(IllegalStateException("activity_missing"))

        val cryptoObject = secureAreaController.getHardwareKeyCryptoObjectForSigning()
        return authenticateForHardwareKey(hostActivity, cryptoObject)
    }

    private suspend fun authenticateForHardwareKey(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject?
    ): Result<Unit> = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        deviceAuthenticationInteractor.getBiometricsAvailability { availability ->
            when (availability) {
                is BiometricsAvailability.CanAuthenticate -> {
                    deviceAuthenticationInteractor.authenticateWithBiometrics(
                        context = activity,
                        crypto = BiometricCrypto(cryptoObject),
                        notifyOnAuthenticationFailure = true,
                        resultHandler = DeviceAuthenticationResult(
                            onAuthenticationSuccess = {
                                if (cont.isActive) cont.resume(Result.success(Unit))
                            },
                            onAuthenticationError = {
                                if (cont.isActive) cont.resume(
                                    Result.failure(IllegalStateException(resourceProvider.genericErrorMessage()))
                                )
                            },
                            onAuthenticationFailure = {
                                if (cont.isActive) cont.resume(
                                    Result.failure(IllegalStateException(resourceProvider.genericErrorMessage()))
                                )
                            }
                        )
                    )
                }

                is BiometricsAvailability.NonEnrolled -> {
                    deviceAuthenticationInteractor.launchBiometricSystemScreen()
                    if (cont.isActive) cont.resume(
                        Result.failure(IllegalStateException(resourceProvider.genericErrorMessage()))
                    )
                }

                is BiometricsAvailability.Failure -> {
                    if (cont.isActive) cont.resume(
                        Result.failure(IllegalStateException(availability.errorMessage))
                    )
                }
            }
        }
    }
}
