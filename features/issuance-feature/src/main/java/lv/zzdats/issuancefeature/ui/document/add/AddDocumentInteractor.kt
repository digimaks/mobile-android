// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.issuancefeature.ui.document.add

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lv.zzdats.authlogic.controller.auth.BiometricsAvailability
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationResult
import lv.zzdats.authlogic.model.BiometricCrypto
import lv.zzdats.authlogic.service.AuthService
import lv.zzdats.businesslogic.extensions.safeAsync
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.zzdats.corelogic.controller.IssuanceMethod
import lv.zzdats.corelogic.controller.IssueDocumentPartialState
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.commonfeature.features.issuance.IssuanceFlowUiConfig
import lv.zzdats.corelogic.controller.AddSampleDataPartialState
import lv.zzdats.corelogic.controller.FetchScopedDocumentsPartialState
import lv.zzdats.corelogic.model.DocumentIdentifier
import lv.zzdats.corelogic.model.toDocumentIdentifier
import lv.zzdats.issuancefeature.ui.document.add.model.DocumentOptionItemUi
import lv.zzdats.networklogic.api.wallet.WalletApiClient
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.uilogic.components.AppIcons
import lv.zzdats.uilogic.serializer.UiSerializer

sealed class AddDocumentInteractorPartialState {
    data class Success(val options: List<DocumentOptionItemUi>) :
        AddDocumentInteractorPartialState()

    data class Failure(val error: String) : AddDocumentInteractorPartialState()
}

interface AddDocumentInteractor {
    fun getAddDocumentOption(flowType: IssuanceFlowUiConfig): Flow<AddDocumentInteractorPartialState>

    fun issueDocument(
        issuanceMethod: IssuanceMethod,
        configId: String
    ): Flow<IssueDocumentPartialState>

    fun handleUserAuth(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult
    )

    fun resumeOpenId4VciWithAuthorization(uri: String)

    fun addSampleData(): Flow<AddSampleDataPartialState>
}

class AddDocumentInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    private val walletApiClient: WalletApiClient,
    private val authService: AuthService,
    ) : AddDocumentInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun getAddDocumentOption(flowType: IssuanceFlowUiConfig): Flow<AddDocumentInteractorPartialState> =
        flow {
            when (val state = walletCoreDocumentsController.getScopedDocuments(resourceProvider.getLocale())) {
                is FetchScopedDocumentsPartialState.Failure -> emit(
                    AddDocumentInteractorPartialState.Failure(
                        error = state.errorMessage
                    )
                )

                is FetchScopedDocumentsPartialState.Success -> {
                    val existingDocs = walletCoreDocumentsController.getAllIssuedDocuments()

                    val options = mutableListOf<DocumentOptionItemUi>()

                    options.addAll(state.documents.mapNotNull { scopedDoc ->
                        if (
                            (flowType != IssuanceFlowUiConfig.NO_DOCUMENT || scopedDoc.isPid)
                            && !scopedDoc.configurationId.contains("jwt", ignoreCase = true)
                        ) {
                            DocumentOptionItemUi(
                                text = scopedDoc.name,
                                icon = AppIcons.Id,
                                configId = scopedDoc.configurationId,
                                available = true,
                                alreadyHave = existingDocs.any { doc ->
                                    doc.name == scopedDoc.name
                                }
                            )
                        } else null
                    })

                    options.add(DocumentOptionItemUi(
                        text = "eSign",
                        icon = AppIcons.Id,
                        configId = DocumentIdentifier.MdocESign.formatType,
                        available = true,
                        alreadyHave = existingDocs.any { doc ->
                            doc.toDocumentIdentifier() == DocumentIdentifier.MdocESign
                        }
                    ))

                    options.add(DocumentOptionItemUi(
                        text = "eSeal",
                        icon = AppIcons.Id,
                        configId = DocumentIdentifier.MdocESeal.formatType,
                        available = true,
                        // Always false because user can have multiple eSeals
                        alreadyHave = false
                    ))

                    emit(AddDocumentInteractorPartialState.Success(options = options))
                }
            }
        }.safeAsync {
            AddDocumentInteractorPartialState.Failure(
                error = it.localizedMessage ?: genericErrorMsg
            )
        }

    override fun addSampleData(): Flow<AddSampleDataPartialState> =
        walletCoreDocumentsController.addSampleData()

    override fun issueDocument(
        issuanceMethod: IssuanceMethod,
        configId: String
    ): Flow<IssueDocumentPartialState> =
        walletCoreDocumentsController.issueDocument(
            issuanceMethod = issuanceMethod,
            configId = configId
        )

    override fun handleUserAuth(
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
}