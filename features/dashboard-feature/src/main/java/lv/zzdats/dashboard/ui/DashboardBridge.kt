// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.dashboardfeature.ui

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationResult
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.commonfeature.config.PresentationMode
import lv.zzdats.commonfeature.config.RequestUriConfig
import lv.zzdats.commonfeature.features.PresentationConfigStore
import lv.zzdats.commonfeature.features.issuance.OfferConfigRepository
import lv.zzdats.commonfeature.features.offer.OfferUiConfig
import lv.zzdats.commonfeature.util.DocumentFieldExtractor.toWebMeta
import lv.zzdats.corelogic.di.getOrCreatePresentationScope
import lv.zzdats.dashboardfeature.interactor.DashboardInteractor
import lv.zzdats.dashboardfeature.interactor.DashboardInteractorGetDocumentsPartialState
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractor
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractorDeleteBookmarkPartialState
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractorDeleteDocumentPartialState
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractorPartialState
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractorReIssueDocumentPartialState
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractorStoreBookmarkPartialState
import lv.zzdats.resourceslogic.bridge.DASHBOARD
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.uilogic.config.ConfigNavigation
import lv.zzdats.uilogic.config.NavigationType
import lv.zzdats.uilogic.navigation.DashboardScreens
import lv.zzdats.uilogic.navigation.DeepLinkType
import lv.zzdats.uilogic.navigation.NavigationCommand
import lv.zzdats.uilogic.navigation.WebNavigationService
import lv.zzdats.uilogic.navigation.hasDeepLink
import lv.zzdats.webbridge.core.BaseBridge
import lv.zzdats.webbridge.core.BridgeRequest
import lv.zzdats.webbridge.core.BridgeResponse

class DashboardBridge(
    private val dashboardInteractor: DashboardInteractor,
    private val documentDetailsInteractor: DocumentDetailsInteractor,
    private val navigationService: WebNavigationService,
    private val resourceProvider: ResourceProvider,
    private val prefKeys: PrefKeys
) : BaseBridge() {

    override fun getName() = DASHBOARD.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when(request.function) {
            DASHBOARD.GET_DOCUMENTS -> handleGetDocuments(request)
            DASHBOARD.GET_DOCUMENT_DETAILS -> handleGetDocumentDetails(request)
            DASHBOARD.DELETE_DOCUMENT -> handleDeleteDocument(request)
            DASHBOARD.REISSUE_DOCUMENT -> handleReIssueDocument(request)
            DASHBOARD.SET_DOCUMENT_FAVORITE -> handleSetDocumentFavorite(request)
            else -> BridgeResponse(
                id = request.id,
                status = BridgeResponse.Status.ERROR,
                error = "Unknown function"
            )
        }
    }

    fun handlePendingDeepLink(uri: Uri) {
        hasDeepLink(uri)?.let { deepLink ->
            when (deepLink.type) {
                DeepLinkType.SIGN_DOCUMENT -> {
                    coroutineScope.launch {
                        val state = dashboardInteractor.getDocuments().first()
                        when (state) {
                            is DashboardInteractorGetDocumentsPartialState.Success -> {
                                if (state.documentsUi.isNotEmpty()) {
                                    val filePath = uri.getQueryParameter("filePath") ?: uri.toString()
                                    val encodedPath = Uri.encode(filePath)
                                    navigationService.navigate(
                                        NavigationCommand.ToWeb("sign/$encodedPath/null")
                                    )
                                } else {
                                    navigationService.navigate(
                                        NavigationCommand.ToWeb("dashboard")
                                    )
                                }
                            }
                            else -> {
                                Log.d("DashboardBridge", "Failed to get documents for deep link handling")
                            }
                        }
                    }
                }
                DeepLinkType.CREDENTIAL_OFFER -> {
                    val offerConfig = OfferUiConfig(
                        offerURI = uri.toString(),
                        onSuccessNavigation = ConfigNavigation(
                            navigationType = NavigationType.PopTo(
                                screen = DashboardScreens.Dashboard
                            )
                        ),
                        onCancelNavigation = ConfigNavigation(
                            navigationType = NavigationType.Pop
                        )
                    )

                    OfferConfigRepository.setOfferConfig(offerConfig)

                    coroutineScope.launch {
                        navigationService.navigate(
                            NavigationCommand.ToWeb("document-offer")
                        )
                    }
                }
                DeepLinkType.OPENID4VP -> {
                    try {
                        getOrCreatePresentationScope().close()
                    } catch (_: Exception) {}

                    PresentationConfigStore.clear()
                    getOrCreatePresentationScope()
                    PresentationConfigStore.setConfig(
                        RequestUriConfig(
                            PresentationMode.OpenId4Vp(
                                uri = uri.toString(),
                                initiatorRoute = DashboardScreens.Dashboard.screenRoute
                            )
                        )
                    )

                    coroutineScope.launch {
                        navigationService.navigate(
                            NavigationCommand.ToWeb("document-presentation")
                        )
                    }
                }
                else -> {
                    Log.d("DashboardBridge", "Unsupported deep link type: ${deepLink.type}")
                }
            }
        }
    }

    private fun handleGetDocuments(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            dashboardInteractor.getDocuments().collect { state ->
                when (state) {
                    is DashboardInteractorGetDocumentsPartialState.Success -> {
                        val documentsData = state.documentsUi.map { document ->
                            mapOf(
                                "meta" to document.toWebMeta(resourceProvider),
                                "documentDetails" to document.documentDetails.map { it.toWebMap() }
                            )
                        }

                        emitEvent(createSuccessResponse(request, mapOf("documents" to documentsData)))
                    }
                    is DashboardInteractorGetDocumentsPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, state.error))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    private fun handleGetDocumentDetails(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val documentId = data["documentId"] as? String
            ?: return createErrorResponse(request, "Missing documentId")

        coroutineScope.launch {
            documentDetailsInteractor.getDocumentDetails(documentId).collect { state ->
                when (state) {
                    is DocumentDetailsInteractorPartialState.Success -> {
                        val document = state.documentUi

                        val responseData = mapOf(
                            "meta" to document.toWebMeta(resourceProvider),
                            "documentDetails" to document.documentDetails.map { it.toWebMap() }
                        )

                        emitEvent(createSuccessResponse(request,responseData))
                    }
                    is DocumentDetailsInteractorPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, state.error))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    private fun handleDeleteDocument(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val documentId = data["documentId"] as? String
            ?: return createErrorResponse(request, "Missing documentId")

        coroutineScope.launch {
            documentDetailsInteractor.deleteDocument(documentId).collect { state ->
                when (state) {
                    is DocumentDetailsInteractorDeleteDocumentPartialState.SingleDocumentDeleted -> {
                        emitEvent(createSuccessResponse(request, mapOf("status" to "deleted")))
                    }
                    is DocumentDetailsInteractorDeleteDocumentPartialState.AllDocumentsDeleted -> {
                        emitEvent(createSuccessResponse(request, mapOf("status" to "all_deleted")))
                    }
                    is DocumentDetailsInteractorDeleteDocumentPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, state.errorMessage))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    private fun handleReIssueDocument(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val documentId = data["documentId"] as? String
            ?: return createErrorResponse(request, "Missing documentId")

        coroutineScope.launch {
            documentDetailsInteractor.reIssueDocument(documentId).collect { state ->
                when (state) {
                    is DocumentDetailsInteractorReIssueDocumentPartialState.Success -> {
                        emitEvent(createSuccessResponse(request, mapOf("status" to "reissued")))
                    }

                    is DocumentDetailsInteractorReIssueDocumentPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, state.errorMessage))
                    }

                    is DocumentDetailsInteractorReIssueDocumentPartialState.UserAuthRequired -> {
                        val activity = findHostFragmentActivity()
                        activity?.let {
                            documentDetailsInteractor.handleUserAuth(
                                context = it,
                                crypto = state.crypto,
                                notifyOnAuthenticationFailure = false,
                                resultHandler = DeviceAuthenticationResult(
                                    onAuthenticationSuccess = {
                                        state.resultHandler.onAuthenticationSuccess()
                                    },
                                    onAuthenticationError = {
                                        state.resultHandler.onAuthenticationError()
                                        emitEvent(
                                            createErrorResponse(
                                                request,
                                                "authentication_error"
                                            )
                                        )
                                    },
                                    onAuthenticationFailure = {
                                        state.resultHandler.onAuthenticationFailure()
                                        emitEvent(
                                            createErrorResponse(
                                                request,
                                                "authentication_error"
                                            )
                                        )
                                    }
                                )
                            )
                        }
                        if (activity == null) {
                            emitEvent(createErrorResponse(request, "activity_missing"))
                        }
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    private fun handleSetDocumentFavorite(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val documentId = data["documentId"] as? String
            ?: return createErrorResponse(request, "Missing documentId")

        val isFavorite = data["isFavorite"] as? Boolean
            ?: return createErrorResponse(request, "Missing isFavorite")

        coroutineScope.launch {
            if (isFavorite) {
                documentDetailsInteractor.storeBookmark(documentId).collect { result ->
                    when (result) {
                        is DocumentDetailsInteractorStoreBookmarkPartialState.Success -> {
                            emitEvent(createSuccessResponse(request, null))
                        }
                        is DocumentDetailsInteractorStoreBookmarkPartialState.Failure -> {
                            emitEvent(createErrorResponse(request, "Failed to store bookmark"))
                        }
                    }
                }
            } else {
                documentDetailsInteractor.deleteBookmark(documentId).collect { result ->
                    when (result) {
                        is DocumentDetailsInteractorDeleteBookmarkPartialState.Success -> {
                            emitEvent(createSuccessResponse(request, null))
                        }
                        is DocumentDetailsInteractorDeleteBookmarkPartialState.Failure -> {
                            emitEvent(createErrorResponse(request, "Failed to delete bookmark"))
                        }
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }
}
