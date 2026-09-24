// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.issuancefeature.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebResourceRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationResult
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.commonfeature.features.issuance.IdentitySelectionRepository
import lv.zzdats.commonfeature.features.issuance.IssuanceFlowUiConfig
import lv.zzdats.commonfeature.features.qr_scan.QrScanFlow
import lv.zzdats.commonfeature.features.qr_scan.QrScanUiConfig
import lv.zzdats.commonfeature.util.convertAnyToDateOnly
import lv.zzdats.corelogic.controller.AddSampleDataPartialState
import lv.zzdats.commonfeature.features.issuance.OfferConfigRepository
import lv.zzdats.issuancefeature.ui.document.add.AddDocumentInteractor
import lv.zzdats.issuancefeature.ui.document.add.AddDocumentInteractorPartialState
import lv.zzdats.issuancefeature.ui.document.offer.DocumentOfferInteractor
import lv.zzdats.issuancefeature.ui.document.offer.IssueDocumentsInteractorPartialState
import lv.zzdats.issuancefeature.ui.document.offer.ResolveDocumentOfferInteractorPartialState
import lv.zzdats.issuancefeature.ui.success.SuccessFetchDocumentByIdPartialState
import lv.zzdats.issuancefeature.ui.success.SuccessInteractor
import lv.zzdats.networklogic.api.wallet.DocumentType
import lv.zzdats.resourceslogic.R
import lv.zzdats.resourceslogic.bridge.DASHBOARD
import lv.zzdats.resourceslogic.bridge.ISSUANCE
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.signfeature.interactor.EParakstIdentitiesInteractor
import lv.zzdats.signfeature.interactor.EParakstIdentitiesState
import lv.zzdats.uilogic.config.ConfigNavigation
import lv.zzdats.uilogic.config.NavigationType
import lv.zzdats.uilogic.navigation.CommonScreens
import lv.zzdats.uilogic.navigation.NavigationCommand.ToNative
import lv.zzdats.uilogic.navigation.NavigationCommand.ToWeb
import lv.zzdats.uilogic.navigation.WebNavigationService
import lv.zzdats.uilogic.navigation.WebScreens
import lv.zzdats.uilogic.navigation.generateComposableArguments
import lv.zzdats.uilogic.navigation.generateComposableNavigationLink
import lv.zzdats.uilogic.serializer.UiSerializer
import lv.zzdats.webbridge.UrlHandler
import lv.zzdats.webbridge.core.BaseBridge
import lv.zzdats.webbridge.core.BridgeRequest
import lv.zzdats.webbridge.core.BridgeResponse
import java.util.Collections

class IssuanceBridge(
    private val addDocumentInteractor: AddDocumentInteractor,
    private val documentOfferInteractor: DocumentOfferInteractor,
    private val navigationService: WebNavigationService,
    private val resourceProvider: ResourceProvider,
    private val successInteractor: SuccessInteractor,
    private val uiSerializer: UiSerializer,
    private val eparakstIdentitiesInteractor: EParakstIdentitiesInteractor,
    private val prefKeys: PrefKeys,
    private val logController: LogController
) : BaseBridge(), UrlHandler {

    private companion object {
        const val TAG = "IssuanceBridge"
        val processedTokens = Collections.synchronizedSet(HashSet<String>())
    }

    private var selectedDocumentType: DocumentType? = null

    override fun getName() = ISSUANCE.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
       return when (request.function) {
            ISSUANCE.GET_SAMPLE_DOCUMENTS -> getSampleDocuments(request)
            ISSUANCE.DELETE_SAMPLE_DOCUMENTS -> deleteAllDocuments(request)
            ISSUANCE.GET_PID_OPTIONS -> handleGetDocumentOptions(request, IssuanceFlowUiConfig.NO_DOCUMENT)
            ISSUANCE.GET_DOCUMENT_OPTIONS -> handleGetDocumentOptions(request, IssuanceFlowUiConfig.EXTRA_DOCUMENT)
            ISSUANCE.ISSUE_DOCUMENT -> handleIssueDocument(request)
            ISSUANCE.RESUME_ISSUANCE -> handleResumeIssuance(request)
            ISSUANCE.GET_PID_DETAILS -> handleGetPidDetails(request)
            ISSUANCE.SCAN_QR_CODE -> handleScanQrCode(request)
            ISSUANCE.ISSUE_DOCUMENT_OFFER -> handleIssueDocumentOffer(request)
            ISSUANCE.RESOLVE_DOCUMENT_OFFER -> handleResolveDocumentOffer(request)
            ISSUANCE.GET_OFFER_CODE_DATA -> handleGetOfferCodeData(request)
            ISSUANCE.SELECT_USER_SIGNATURES -> handleSelectEParakstIdentities(request)
            ISSUANCE.GET_USER_SIGNATURE_OPTIONS -> handleGetSelectionOptions(request)
            ISSUANCE.LAUNCH_SEB_ACTIVITY -> handleLaunchSEB(request)
            else -> createErrorResponse(request, "Unknown function ${request.function}")
        }
    }

    private fun handleGetDocumentOptions(request: BridgeRequest, flowType: IssuanceFlowUiConfig): BridgeResponse {
        val response = BridgeResponse(
            id = request.id,
            status = BridgeResponse.Status.SUCCESS
        )

        coroutineScope.launch {
            addDocumentInteractor.getAddDocumentOption(flowType)
                .collect { state ->
                    when(state) {
                        is AddDocumentInteractorPartialState.Success -> {
                            val options = state.options.map { option ->
                                mapOf(
                                    "text" to option.text,
                                    "type" to option.configId,
                                    "exists" to "${option.alreadyHave}"
                                )
                            }

                            val response = mapOf(
                                "options" to options
                            )
                            emitEvent(createSuccessResponse(request, response))
                        }
                        is AddDocumentInteractorPartialState.Failure -> {
                            emitEvent(createErrorResponse(request, state.error))
                        }
                    }
                }
        }

        return response
    }

    @SuppressLint("RestrictedApi")
    private fun handleIssueDocument(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val rawDocType = data["documentType"] as? String
            ?: return createErrorResponse(request, "Missing document type")

        val documentType = when (rawDocType) {
            "eu.europa.ec.eudi.mdl_mdoc" -> DocumentType.MDL
            "eu.europa.ec.eudi.pid_mdoc" -> DocumentType.PID
            "eu.europa.ec.eudi.rtu_diploma_mdoc" -> DocumentType.RTU
            "eu.digimaks.eseal" -> DocumentType.ESEAL
            "eu.digimaks.esign" -> DocumentType.ESIGN
            "eu.europa.ec.eudi.iban" -> DocumentType.IBAN
            else -> return createErrorResponse(request, "Unsupported document type")
        }

        selectedDocumentType = documentType

        emitEvent(createSuccessResponse(request, null))

        coroutineScope.launch {
            prefKeys.setLastDocType(documentType.type)
            when (documentType) {
                DocumentType.ESEAL, DocumentType.ESIGN -> {
                    handleEParakstFlow(request)
                }
                else -> {
                    val activity = findHostFragmentActivity()
                    documentOfferInteractor.issueDocumentByType(documentType, activity)
                        .collect { result ->
                            when (result) {
                                is IssueDocumentsInteractorPartialState.DeferredSuccess -> TODO()
                                is IssueDocumentsInteractorPartialState.Failure -> {
                                    logController.e(TAG) { "Issue document failed for type=${documentType.type}: ${result.errorCode ?: result.errorMessage}" }
                                    result.cause?.let { logController.e(TAG, it) }
                                    val bridgeError = result.errorCode ?: result.errorMessage
                                    navigationService.navigate(
                                        ToWeb(
                                            path = "document-offer-manual/error?error=${result.errorMessage}",
                                        )
                                    )
                                    emitEvent(createErrorResponse(request, bridgeError))
                                }
                                is IssueDocumentsInteractorPartialState.Success -> {
                                    navigationService.navigate(
                                        ToWeb(
                                            path = "document-offer-manual/success",
                                        )
                                    )
                                    prefKeys.setLastDocType("")
                                    emitEvent(createSuccessResponse(request, null))
                                }
                                is IssueDocumentsInteractorPartialState.UserAuthRequired -> {
                                    val activity = findHostFragmentActivity()
                                    activity?.let {
                                        addDocumentInteractor.handleUserAuth(
                                            context = it,
                                            crypto = result.crypto,
                                            notifyOnAuthenticationFailure = false,
                                            resultHandler = DeviceAuthenticationResult(
                                                onAuthenticationSuccess = {
                                                    result.resultHandler.onAuthenticationSuccess()
                                                    prefKeys.setLastDocType("")
                                                },
                                                onAuthenticationError = {
                                                    result.resultHandler.onAuthenticationError()
                                                    prefKeys.setLastDocType("")
                                                    emitEvent(
                                                        createErrorResponse(
                                                            request,
                                                            "authentication_error"
                                                        )
                                                    )
                                                },
                                                onAuthenticationFailure = {
                                                    result.resultHandler.onAuthenticationFailure()
                                                    prefKeys.setLastDocType("")
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
                                        logController.e(TAG) { "Issue document authentication failed: host activity missing" }
                                        emitEvent(createErrorResponse(request, "activity_missing"))
                                    }
                                }
                            }
                        }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    override fun handleUrl(request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        if (url.contains("/simple-sign/v2/mobile/identitiesResult/")) {
            val token = url.substringAfterLast("/")

            synchronized(Companion::class.java) {
                if (!processedTokens.contains(token)) {
                    processedTokens.add(token)

                    eparakstIdentitiesInteractor.handleIdentitiesResult(token)
                } else {
                }
            }
            return true
        }

        if (url.contains("auth-done/?error")) {
            logController.e(TAG) { "External auth callback reported error url=$url" }
            coroutineScope.launch {
                navigationService.navigate(ToWeb("document-offer-manual/error"))
            }
            return true
        }
        return false
    }

    private fun handleGetSelectionOptions(request: BridgeRequest): BridgeResponse {
        val options = IdentitySelectionRepository.getSelectionOptions()
            ?: return createErrorResponse(request, null)

        val identitiesMap = when(selectedDocumentType) {
            DocumentType.ESIGN -> mapOf(
                "eSign" to options.eSign.map { identity ->
                    mapOf(
                        "id" to identity.Sid,
                        "name" to identity.cn,
                        "expiresOn" to formatIdentityDate(identity.ExpiresOn),
                        "issuedOn" to formatIdentityDate(identity.IssuedOn),
                        "type" to "eSign"
                    )
                }
            )
            DocumentType.ESEAL -> mapOf(
                "eSeal" to options.eSeal.map { identity ->
                    mapOf(
                        "id" to identity.Sid,
                        "name" to identity.cn,
                        "expiresOn" to formatIdentityDate(identity.ExpiresOn),
                        "issuedOn" to formatIdentityDate(identity.IssuedOn),
                        "type" to "eSeal"
                    )
                }
            )
            else -> emptyMap()
        }

        val response = mapOf(
            "options" to identitiesMap
        )

        emitEvent(createSuccessResponse(request, response))

        return createSuccessResponse(request, mapOf("options" to identitiesMap))
    }

    private fun formatIdentityDate(value: String): String {
        return convertAnyToDateOnly(value) ?: value
    }

    private suspend fun handleEParakstFlow(request: BridgeRequest) {
        eparakstIdentitiesInteractor.initiateEParakstsIdentities(selectedDocumentType!!)
            .collect { state ->
                when (state) {
                    is EParakstIdentitiesState.RedirectToAuth -> {
                        withContext(Dispatchers.Main) {
                            webView?.loadUrl(state.url)
                        }
                    }
                    is EParakstIdentitiesState.SelectionNeeded -> {
                        IdentitySelectionRepository.setSelectionOptions(state.response)

                        withContext(Dispatchers.Main) {
                            navigationService.navigate(
                                ToWeb(
                                    path = "signature-select",
                                )
                            )
                        }
                        emitEvent(createSuccessResponse(request, null))
                    }
                    is EParakstIdentitiesState.Success -> {
                        emitEvent(createSuccessResponse(request, state.response))
                    }
                    is EParakstIdentitiesState.Complete -> {
                        withContext(Dispatchers.Main) {
                            navigationService.navigate(
                                ToWeb(
                                    path = "document-offer-manual/success",
                                )
                            )
                        }
                    }
                    is EParakstIdentitiesState.Failure -> {
                        logController.e(TAG) { "eParaksts identities flow failed: ${state.error}" }
                        navigationService.navigate(
                            ToWeb(
                                path = "document-offer-manual/error",
                            )
                        )
                        emitEvent(createErrorResponse(request, state.error))
                    }
                    else -> {}
                }
            }
    }

    private fun handleSelectEParakstIdentities(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val selectedIds = data["selectedIds"] as? List<*>
            ?: return createErrorResponse(request, "Missing selectedIds")

        val options = IdentitySelectionRepository.getSelectionOptions()
            ?: return createErrorResponse(request, "No selection options available")

        IdentitySelectionRepository.clearSelectionOptions()

        coroutineScope.launch {
            eparakstIdentitiesInteractor.storeSelectedIdentities(
                selectedIds.filterIsInstance<String>(),
                options,
                selectedDocumentType!!
            )
        }

        return createSuccessResponse(request, null)
    }

    private fun handleScanQrCode(request: BridgeRequest): BridgeResponse {
        emitEvent(createSuccessResponse(request, null))
        coroutineScope.launch {
            navigationService.navigate(
                ToNative(
                    generateComposableNavigationLink(
                        CommonScreens.QrScan,
                        generateComposableArguments(
                            mapOf(
                                QrScanUiConfig.serializedKeyName to uiSerializer.toBase64(
                                    QrScanUiConfig(
                                        title = resourceProvider.getString(R.string.issuance_qr_scan_title),
                                        subTitle = resourceProvider.getString(R.string.issuance_qr_scan_subtitle),
                                        qrScanFlow = QrScanFlow.Issuance(IssuanceFlowUiConfig.EXTRA_DOCUMENT)
                                    ),
                                    QrScanUiConfig.Parser
                                )
                            )
                        )
                    )
                )
            )
        }
        return createSuccessResponse(request, null)
    }

    private fun handleGetPidDetails(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            successInteractor.fetchMainPidDocument().collect { response ->
                when (response) {
                    is SuccessFetchDocumentByIdPartialState.Success -> {
                        emitEvent(createSuccessResponse(
                            request,
                            mapOf("firstName" to response.fullName.split(" ").firstOrNull().orEmpty())
                        ))
                    }
                    is SuccessFetchDocumentByIdPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, response.error))
                    }
                }
            }
        }
        return createSuccessResponse(request, null)
    }

    private fun handleResumeIssuance(request: BridgeRequest): BridgeResponse {
        val uri = (request.data as? Map<*, *>)?.get("uri") as? String
            ?: return createErrorResponse(request, "Invalid URI")

        coroutineScope.launch {
            addDocumentInteractor.resumeOpenId4VciWithAuthorization(uri)
        }

        return createSuccessResponse(request, null)
    }

    @SuppressLint("RestrictedApi")
    private fun handleIssueDocumentOffer(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val offerUri = data["offerUri"] as? String
            ?: return createErrorResponse(request, "Missing offerUri")

        val txCode = data["txCode"] as? String
        val issuerName = data["issuerName"] as? String ?: ""

        val onSuccessNavigation = ConfigNavigation(
            navigationType = NavigationType.PushRoute(
                route = WebScreens.Main.screenRoute
            )
        )

        coroutineScope.launch {
            val activity = findHostFragmentActivity()
            documentOfferInteractor.issueDocuments(
                offerUri = offerUri,
                txCode = txCode,
                issuerName = issuerName,
                navigation = onSuccessNavigation,
                activity = activity
            ).collect { result ->
                when (result) {
                    is IssueDocumentsInteractorPartialState.Success -> {
                        emitEvent(createSuccessResponse(request, null))
                    }
                    is IssueDocumentsInteractorPartialState.Failure -> {
                        logController.e(TAG) { "Issue document offer failed: ${result.errorCode ?: result.errorMessage}" }
                        result.cause?.let { logController.e(TAG, it) }
                        val bridgeError = result.errorCode ?: result.errorMessage
                        emitEvent(createErrorResponse(request, bridgeError))
                    }
                    is IssueDocumentsInteractorPartialState.DeferredSuccess -> {
                        navigationService.navigate(ToWeb(ISSUANCE.SCREENS.DOCUMENT_DEFERRED_SUCCESS))
                        emitEvent(createSuccessResponse(request, null))
                    }
                    is IssueDocumentsInteractorPartialState.UserAuthRequired -> {
                        val activity = findHostFragmentActivity()
                        activity?.let {
                            addDocumentInteractor.handleUserAuth(
                                context = it,
                                crypto = result.crypto,
                                notifyOnAuthenticationFailure = false,
                                resultHandler = DeviceAuthenticationResult(
                                    onAuthenticationSuccess = {
                                        result.resultHandler.onAuthenticationSuccess()
                                    },
                                    onAuthenticationError = {
                                        result.resultHandler.onAuthenticationError()
                                        emitEvent(
                                            createErrorResponse(
                                                request,
                                                "authentication_error"
                                            )
                                        )
                                    },
                                    onAuthenticationFailure = {
                                        result.resultHandler.onAuthenticationFailure()
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
                            logController.e(TAG) { "Issue document offer authentication failed: host activity missing" }
                            emitEvent(createErrorResponse(request, "activity_missing"))
                        }
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    private fun handleResolveDocumentOffer(request: BridgeRequest): BridgeResponse {
        val offerConfig = OfferConfigRepository.getOfferConfig(clear = false)
            ?: return createErrorResponse(request, "Offer config not found")

        val offerUri = offerConfig.offerURI

        coroutineScope.launch {
            documentOfferInteractor.resolveDocumentOffer(offerUri).collect { result ->
                when (result) {
                    is ResolveDocumentOfferInteractorPartialState.Success -> {

                        val response = mapOf(
                            "documents" to result.documents.map { doc ->
                                mapOf(
                                    "title" to doc.title
                                )
                            },
                            "issuerName" to result.issuerName,
                            "txCodeLength" to result.txCodeLength,
                            "offerUri" to offerUri,
                        )

                        emitEvent(createSuccessResponse(request, response))
                    }
                    is ResolveDocumentOfferInteractorPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, result.errorMessage))
                    }

                    is ResolveDocumentOfferInteractorPartialState.NoDocument -> {
                        emitEvent(createErrorResponse(request, "No Document"))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    private fun handleGetOfferCodeData(request: BridgeRequest): BridgeResponse {
        val offerConfig = OfferConfigRepository.getOfferConfig()
            ?: return createErrorResponse(request, "Offer config not found")

        coroutineScope.launch {
            documentOfferInteractor.resolveDocumentOffer(offerConfig.offerURI).collect { result ->
                when (result) {
                    is ResolveDocumentOfferInteractorPartialState.Success -> {
                        val responseData = mapOf(
                            "offerUri" to offerConfig.offerURI,
                            "issuerName" to result.issuerName,
                            "txCodeLength" to result.txCodeLength
                        )
                        emitEvent(createSuccessResponse(request, responseData))
                    }
                    is ResolveDocumentOfferInteractorPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, result.errorMessage))
                    }
                    else -> {
                        emitEvent(createErrorResponse(request, "Failed to get offer data"))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    private fun getSampleDocuments(request: BridgeRequest): BridgeResponse {
        CoroutineScope(Dispatchers.IO).launch {
            addDocumentInteractor.addSampleData().collect { response ->
                when (response) {
                    is AddSampleDataPartialState.Success -> {
                        navigationService.navigate(ToWeb(path = DASHBOARD.SCREENS.MAIN))
                        emitEvent(createSuccessResponse(request, null))
                    }

                    is AddSampleDataPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, response.error))
                    }
                }
            }
        }

        return createSuccessResponse(request, null)
    }

    private fun deleteAllDocuments(request: BridgeRequest): BridgeResponse {
        return createSuccessResponse(request, null)
    }

    private fun handleLaunchSEB(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            try {
                val context = resourceProvider.provideContext()
                val packageName = "lt.seb.mob.lv"
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)

                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    emitEvent(createSuccessResponse(request, null))
                } else {
                    val explicitIntent = Intent()
                    explicitIntent.setClassName(
                        packageName,
                        "baltic.seb.android.mainActivity.MainActivity"
                    )
                    explicitIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(explicitIntent)
                        emitEvent(createSuccessResponse(request, null))
                    } catch (e: Exception) {
                        emitEvent(createErrorResponse(request, "Explicit launch failed: ${e.message}"))
                    }
                }
            } catch (e: Exception) {
                emitEvent(createErrorResponse(request, "Failed to launch SEB app: ${e.message}"))
            }
        }

        return createSuccessResponse(request, null)
    }
}
