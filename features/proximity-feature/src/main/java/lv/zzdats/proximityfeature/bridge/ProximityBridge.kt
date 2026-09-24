// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.proximityfeature.bridge

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lv.zzdats.proximityfeature.interactor.ProximityLoadingInteractor
import lv.zzdats.proximityfeature.interactor.ProximityLoadingObserveResponsePartialState
import lv.zzdats.proximityfeature.interactor.ProximityLoadingSendRequestedDocumentPartialState
import lv.zzdats.proximityfeature.interactor.ProximityQRInteractor
import lv.zzdats.proximityfeature.interactor.ProximityQRPartialState
import lv.zzdats.proximityfeature.interactor.ProximityRequestInteractor
import lv.zzdats.proximityfeature.interactor.ProximityRequestPartialState
import lv.zzdats.proximityfeature.interactor.ProximitySuccessInteractor
import lv.zzdats.proximityfeature.interactor.ProximitySuccessPartialState
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationResult
import lv.zzdats.authlogic.model.BiometricCrypto
import lv.zzdats.commonfeature.config.RequestUriConfig
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.zzdats.commonfeature.features.request.transformer.PresentationDocument
import lv.zzdats.commonfeature.util.DocumentFieldExtractor.toWebMeta
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.model.AuthenticationData
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractor
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractorPartialState
import lv.zzdats.resourceslogic.bridge.PROXIMITY
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.uilogic.navigation.NavigationCommand.ToWeb
import lv.zzdats.uilogic.navigation.WebNavigationService
import lv.zzdats.uilogic.serializer.UiSerializer
import lv.zzdats.webbridge.core.BaseBridge
import lv.zzdats.webbridge.core.BridgeRequest
import lv.zzdats.webbridge.core.BridgeResponse
import kotlin.collections.lastIndex

class ProximityBridge(
    private val qrInteractor: ProximityQRInteractor,
    private val requestInteractor: ProximityRequestInteractor,
    private val loadingInteractor: ProximityLoadingInteractor,
    private val successInteractor: ProximitySuccessInteractor,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val navigationService: WebNavigationService,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    private val addDocumentInteractor: DocumentDetailsInteractor,
    private val walletCoreDocumentsController: WalletCoreDocumentsController
) : BaseBridge() {

    override fun getName(): String = PROXIMITY.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when (request.function) {
            PROXIMITY.START_QR_ENGAGEMENT -> handleStartQrEngagement(request)
            PROXIMITY.GET_REQUEST_DOCUMENTS -> handleGetRequestDocuments(request)
            PROXIMITY.UPDATE_REQUESTED_DOCUMENTS -> handleUpdateRequestedDocuments(request)
            PROXIMITY.SEND_REQUESTED_DOCUMENTS -> handleSendRequestedDocuments(request)
            PROXIMITY.OBSERVE_RESPONSE -> handleObserveResponse(request)
            PROXIMITY.GET_SUCCESS_DATA -> handleGetSuccessData(request)
            PROXIMITY.SET_CONFIG -> handleSetConfig(request)
            PROXIMITY.CANCEL_TRANSFER -> handleCancelTransfer(request)
            PROXIMITY.STOP_PRESENTATION -> handleStopPresentation(request)
            PROXIMITY.START_PRESENTATION -> handleStartPresentation(request)
            PROXIMITY.TOGGLE_NFC -> handleToggleNfc(request)
            else -> createErrorResponse(request, "Unknown function: ${request.function}")
        }
    }

    private fun handleStartQrEngagement(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            qrInteractor.startQrEngagement().collect { state ->
                when (state) {
                    is ProximityQRPartialState.QrReady -> {
                        val responseData = mapOf(
                            "qrCode" to state.qrCode,
                            "status" to "ready"
                        )
                        emitEvent(createSuccessResponse(request, responseData))
                    }
                    is ProximityQRPartialState.Connected -> {
                        navigationService.navigate(ToWeb(PROXIMITY.SCREENS.REQUEST))
                        emitEvent(createSuccessResponse(request, mapOf("status" to "connected")))
                    }
                    is ProximityQRPartialState.Error -> {
                        emitEvent(createErrorResponse(request, state.error))
                    }
                    is ProximityQRPartialState.Disconnected -> {
                        handleCleanup()
                        emitEvent(createErrorResponse(request, "DISCONNECTED"))
                    }
                }
            }
        }
        return createSuccessResponse(request, "QR engagement started")
    }

    private fun handleGetRequestDocuments(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            requestInteractor.getRequestDocuments().collect { state ->
                when (state) {
                    is ProximityRequestPartialState.Success -> {
                        val documentsForJson = state.requestDocuments.map { doc ->
                            val fieldsForJson = doc.claims.map { claim ->
                                mapOf(
                                    "id" to claim.id,
                                    "readableName" to claim.displayTitle,
                                    "value" to claim.value,
                                    "checked" to claim.isChecked,
                                    "enabled" to claim.isEnabled,
                                    "elementIdentifier" to claim.elementIdentifier,
                                    "isRequired" to claim.isRequired
                                )
                            }

                            var meta: Map<String, Any> = emptyMap()
                            val storedDocument =
                                walletCoreDocumentsController.getAllIssuedDocuments()
                                    .firstOrNull { it.name == doc.docName }

                            addDocumentInteractor.getDocumentDetails(storedDocument!!.id)
                                .collect { state ->
                                    when (state) {
                                        is DocumentDetailsInteractorPartialState.Success -> {
                                            val document = state.documentUi
                                            meta = document.toWebMeta(resourceProvider) as Map<String, Any>
                                        }

                                        else -> {}
                                    }
                                }

                            mapOf(
                                "title" to doc.docName,
                                "meta" to meta,
                                "fields" to fieldsForJson
                            )
                        }

                        val responseData = mapOf(
                            "verifierName" to state.verifierName,
                            "verifierIsTrusted" to state.verifierIsTrusted,
                            "documents" to documentsForJson,
                            "requestPurpose" to state.requestPurpose
                        )
                        emitEvent(createSuccessResponse(request, responseData))
                    }
                    is ProximityRequestPartialState.NoData -> {
                        val responseData = mapOf(
                            "verifierName" to state.verifierName,
                            "verifierIsTrusted" to state.verifierIsTrusted,
                            "documents" to emptyList<Map<String, Any>>()
                        )
                        emitEvent(createSuccessResponse(request, responseData))
                    }
                    is ProximityRequestPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, state.error))
                    }
                    is ProximityRequestPartialState.Loading -> {
                        emitEvent(createSuccessResponse(request, mapOf("status" to "loading")))
                    }
                    is ProximityRequestPartialState.Disconnect -> {
                        handleCleanup()
                        emitEvent(createErrorResponse(request, "DISCONNECTED"))
                    }
                }
            }
        }
        return createSuccessResponse(request, "Observing request documents")
    }

    private fun handleUpdateRequestedDocuments(request: BridgeRequest): BridgeResponse {
//        val data = request.data as? Map<*, *>
//            ?: return createErrorResponse(request, "Invalid request data")
//
//        val documents = data["documents"] as? List<*>
//            ?: return createErrorResponse(request, "Missing documents")
//
//        try {
//            val updatedDocuments = documents.mapNotNull { doc ->
//                val docMap = doc as? Map<*, *> ?: return@mapNotNull null
//                val fields = docMap["fields"] as? List<*> ?: return@mapNotNull null
//
//                val updatedFields = fields.mapNotNull { field ->
//                    val fieldMap = field as? Map<*, *> ?: return@mapNotNull null
//                    PresentationDocument.Claim(
//                        id = fieldMap["id"] as? String ?: "",
//                        displayTitle = fieldMap["readableName"] as? String ?: "",
//                        value = fieldMap["value"] as? String ?: "",
//                        isChecked = fieldMap["checked"] as? Boolean ?: false,
//                        isEnabled = fieldMap["enabled"] as? Boolean ?: true,
//                        elementIdentifier = fieldMap["elementIdentifier"] as? String ?: "",
//                        isRequired = fieldMap["isRequired"] as? Boolean ?: false
//                    )
//                }
//
//                PresentationDocument(
//                    docId = docMap["docId"] as? String ?: "",
//                    docName = docMap["title"] as? String ?: "",
//                    claims = updatedFields,
//                )
//            }
//
//            coroutineScope.launch {
//                requestInteractor.updateRequestedDocuments(updatedDocuments)
//            }
//
            return createSuccessResponse(request, null)
//        } catch (e: Exception) {
//            return createErrorResponse(request, "Failed to update documents: ${e.message}")
//        }
    }

    @SuppressLint("RestrictedApi")
    private fun handleSendRequestedDocuments(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            loadingInteractor.observeResponse().collect { state ->
                when (state) {
                    is ProximityLoadingObserveResponsePartialState.UserAuthenticationRequired -> {
                        handleUserAuthentication(request, state.authenticationData)
                    }
                    is ProximityLoadingObserveResponsePartialState.RequestReadyToBeSent -> {
                        navigationService.navigate(ToWeb(PROXIMITY.SCREENS.LOADING))

                        when (val result = loadingInteractor.sendRequestedDocuments()) {
                            is ProximityLoadingSendRequestedDocumentPartialState.Success -> {
                                // Success is handled by the ongoing observeResponse collector
                            }
                            is ProximityLoadingSendRequestedDocumentPartialState.Failure -> {
                                emitEvent(createErrorResponse(request, result.error))
                                handleCleanup()
                            }
                        }
                    }
                    is ProximityLoadingObserveResponsePartialState.Success -> {
                        navigationService.navigate(ToWeb(PROXIMITY.SCREENS.SUCCESS))
                        emitEvent(createSuccessResponse(request, mapOf("status" to "success")))
                        handleCleanup()
                    }
                    is ProximityLoadingObserveResponsePartialState.Failure -> {
                        emitEvent(createErrorResponse(request, state.error))
                        handleCleanup()
                    }
                }
            }
        }
        return createSuccessResponse(request, "Document sending initiated")
    }

    private fun handleUserAuthentication(
        request: BridgeRequest,
        authDataList: List<AuthenticationData>,
        index: Int = 0
    ) {
        if (index >= authDataList.size) {
            // All authentications are done, now send the documents
            coroutineScope.launch {
                loadingInteractor.sendRequestedDocuments()
            }
            return
        }

        val authData = authDataList[index]
        val isFinalAuthentication = index == authDataList.lastIndex
        val activity = findHostFragmentActivity()
        if (activity == null) {
            emitEvent(createErrorResponse(request, "activity_missing"))
            return
        }

        deviceAuthenticationInteractor.authenticateWithBiometrics(
            context = activity,
            crypto = authData.crypto,
            notifyOnAuthenticationFailure = true,
            resultHandler = DeviceAuthenticationResult(
                onAuthenticationSuccess = {
                    authData.onAuthenticationSuccess()
                    if (isFinalAuthentication) {
                        coroutineScope.launch {
                            loadingInteractor.sendRequestedDocuments()
                        }
                    } else {
                        coroutineScope.launch {
                            delay(250)
                            handleUserAuthentication(request, authDataList, index + 1)
                        }
                    }
                },
                onAuthenticationError = {
                    emitEvent(createErrorResponse(request, "authentication_failed"))
                }
            )
        )
    }

    private fun handleObserveResponse(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            loadingInteractor.observeResponse().collect { state ->
                val responseData = when (state) {
                    is ProximityLoadingObserveResponsePartialState.Success -> mapOf(
                        "status" to "success",
                        "message" to "Documents shared successfully"
                    )
                    is ProximityLoadingObserveResponsePartialState.RequestReadyToBeSent -> mapOf(
                        "status" to "ready_to_send",
                        "message" to "Ready to send documents"
                    )
                    is ProximityLoadingObserveResponsePartialState.Failure -> mapOf(
                        "status" to "error",
                        "error" to state.error
                    )
                    is ProximityLoadingObserveResponsePartialState.UserAuthenticationRequired -> mapOf(
                        "status" to "auth_required",
                        "message" to "User authentication required"
                    )
                }

                emitEvent(createSuccessResponse(request, responseData))
            }
        }
        return createSuccessResponse(request, "Observing loading response")
    }

    private fun handleGetSuccessData(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            successInteractor.getSuccessData().collect { state ->
                when (state) {
                    is ProximitySuccessPartialState.Success -> {
                        val responseData = mapOf(
                            "verifierName" to state.data.verifierName,
                            "verifierIsTrusted" to state.data.verifierIsTrusted,
                            "documentsSharedCount" to state.data.documentsSharedCount,
                            "timestamp" to state.data.timestamp
                        )
                        emitEvent(createSuccessResponse(request, responseData))
                    }
                    is ProximitySuccessPartialState.Failure -> {
                        emitEvent(createErrorResponse(request, state.error))
                    }
                }
            }
        }
        return createSuccessResponse(request, "Getting success data")
    }

    private fun handleSetConfig(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        try {
            val configString = data["config"] as? String
                ?: return createErrorResponse(request, "Missing config")

            val config = uiSerializer.fromBase64(
                configString,
                RequestUriConfig::class.java,
                RequestUriConfig.Parser
            ) ?: return createErrorResponse(request, "Invalid config format")

            coroutineScope.launch {
                qrInteractor.setConfig(config)
                requestInteractor.setConfig(config)
            }

            return createSuccessResponse(request, null)
        } catch (e: Exception) {
            return createErrorResponse(request, "Failed to set config: ${e.message}")
        }
    }

    private fun handleCancelTransfer(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            qrInteractor.cancelTransfer()
            handleCleanup()
        }

        coroutineScope.launch {
            navigationService.navigate(ToWeb("dashboard"))
        }
        return createSuccessResponse(request, null)
    }

    private fun handleStopPresentation(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            requestInteractor.stopPresentation()
            successInteractor.stopPresentation()
            handleCleanup()
        }

        coroutineScope.launch {
            navigationService.navigate(ToWeb("dashboard"))
        }
        return createSuccessResponse(request, null)
    }

    private fun handleStartPresentation(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            navigationService.navigate(ToWeb(PROXIMITY.SCREENS.QR))
        }
        return createSuccessResponse(request, "Presentation started")
    }

    private fun handleToggleNfc(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val enable = data["enable"] as? Boolean
            ?: return createErrorResponse(request, "Missing enable parameter")

        val activity = findHostActivity() ?: return createErrorResponse(request, "Activity not found")


        try {
            qrInteractor.toggleNfcEngagement(activity, enable)
            return createSuccessResponse(request, mapOf("nfcEnabled" to enable))
        } catch (e: Exception) {
            return createErrorResponse(request, "Failed to toggle NFC: ${e.message}")
        }
    }

    private fun handleCleanup() {
        coroutineScope.launch {
            try {
                requestInteractor.cleanup()
                successInteractor.stopPresentation()
            } catch (_: Exception) {
                // no-op
            }
        }
    }
}
