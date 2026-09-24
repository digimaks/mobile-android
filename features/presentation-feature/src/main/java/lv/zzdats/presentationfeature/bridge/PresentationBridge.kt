// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.presentationfeature.bridge

import android.annotation.SuppressLint
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationPolicy
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationResult
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.commonfeature.features.PresentationConfigStore
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.zzdats.commonfeature.features.qr_scan.QrScanFlow
import lv.zzdats.commonfeature.features.qr_scan.QrScanUiConfig
import lv.zzdats.commonfeature.features.request.transformer.PresentationDocument
import lv.zzdats.commonfeature.util.DocumentFieldExtractor.toWebMeta
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.controller.WalletCorePresentationController
import lv.zzdats.corelogic.di.getOrCreatePresentationScope
import lv.zzdats.corelogic.model.AuthenticationData
import lv.zzdats.corelogic.model.toDocumentIdentifier
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractor
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractorPartialState
import lv.zzdats.presentationfeature.interactor.PresentationLoadingInteractor
import lv.zzdats.presentationfeature.interactor.PresentationLoadingObserveResponsePartialState
import lv.zzdats.presentationfeature.interactor.PresentationLoadingSendRequestedDocumentPartialState
import lv.zzdats.presentationfeature.interactor.PresentationRequestInteractor
import lv.zzdats.presentationfeature.interactor.PresentationRequestInteractorPartialState
import lv.zzdats.resourceslogic.bridge.PRESENTATION
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.uilogic.navigation.CommonScreens
import lv.zzdats.uilogic.navigation.DashboardScreens
import lv.zzdats.uilogic.navigation.NavigationCommand.ToNative
import lv.zzdats.uilogic.navigation.NavigationCommand.ToWeb
import lv.zzdats.uilogic.navigation.WebNavigationService
import lv.zzdats.uilogic.navigation.generateComposableArguments
import lv.zzdats.uilogic.navigation.generateComposableNavigationLink
import lv.zzdats.uilogic.serializer.UiSerializer
import lv.zzdats.webbridge.core.BaseBridge
import lv.zzdats.webbridge.core.BridgeRequest
import lv.zzdats.webbridge.core.BridgeResponse
import lv.zzdats.resourceslogic.R
import lv.zzdats.storagelogic.dao.TransactionLogDao
import lv.zzdats.storagelogic.model.TransactionLog
import lv.zzdats.storagelogic.model.TransactionType
import lv.zzdats.uilogic.navigation.NavigationCommand.ToExternal
import org.json.JSONArray
import org.json.JSONObject

class PresentationBridge(
    private val navigationService: WebNavigationService,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val addDocumentInteractor: DocumentDetailsInteractor,
    private val prefKeys: PrefKeys,
    private val transactionLogDao: TransactionLogDao,
) : BaseBridge() {


    private val presentationScope get() = getOrCreatePresentationScope()
    private val presentationRequestInteractor get() = presentationScope.get<PresentationRequestInteractor>()
    private val presentationLoadingInteractor get() = presentationScope.get<PresentationLoadingInteractor>()

    private var currentDocuments: List<PresentationDocument> = emptyList()
    private var lastSuccessfulSelection: RememberedPresentationSelection? = null
    private var pendingConfirmedSelection: RememberedPresentationSelection? = null

    private data class RememberedPresentationSelection(
        val selectedDocumentId: String,
        val selectedFieldIds: Set<String>,
    )

    private data class StoredPresentationSelection(
        val selectedFieldIds: Set<String>,
        val updatedAt: Long? = null,
    )

    override fun getName() = PRESENTATION.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when(request.function) {
            PRESENTATION.GET_REQUEST_DOCUMENTS -> {
                getOrCreatePresentationScope()
                resetPresentationSelectionState()
                val config = PresentationConfigStore.getConfig()
                if (config == null) {
                    emitEvent(createErrorResponse(request, "No presentation config found"))
                    return createErrorResponse(request, "No presentation config found")
                }
                presentationRequestInteractor.setConfig(config)
                handleGetRequestDocuments(request)
            }
            PRESENTATION.CANCEL_REQUEST -> {
                coroutineScope.launch {
                    logCanceledPresentationTransaction()
                }
                cleanupPresentation()
                emitEvent(createSuccessResponse(request, null))
                createSuccessResponse(request, null)
            }
            PRESENTATION.CONFIRM_REQUEST -> handleConfirmRequest(request)
            PRESENTATION.SET_VENDOR_PRESENTATION_PREFERENCE -> handleSetVendorPresentationPreference(request)
            PRESENTATION.START_PRESENTATION -> handleStartPresentation(request)
            else -> createErrorResponse(request, "Unknown function")
        }
    }

    private fun updateCurrentDocuments(documents: List<PresentationDocument>) {
        currentDocuments = documents
    }

    private fun handleStartPresentation(request: BridgeRequest): BridgeResponse {
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
                                        title = "Present Documents",
                                        subTitle = "Scan verifier's QR code",
                                        qrScanFlow = QrScanFlow.Presentation
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

    private fun handleConfirmRequest(request: BridgeRequest): BridgeResponse {

        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid data")

        val selectedDocumentId = data["selectedDocumentId"] as? String
            ?: return createErrorResponse(request, "Missing selectedDocumentId")

        val fields = data["fields"] as? Map<*, *>
            ?: return createErrorResponse(request, "Missing fields data")

        val updatedItems = currentDocuments.map { doc ->
            if (doc.docId != selectedDocumentId) {
                doc.copy(claims = doc.claims.map { claim -> claim.copy(isChecked = false) })
            } else {
                doc.copy(
                    claims = doc.claims.map { claim ->
                        val checkedStatusFromRequest = fields[claim.id] as? Boolean
                        if (checkedStatusFromRequest != null && claim.isEnabled) {
                            claim.copy(isChecked = checkedStatusFromRequest)
                        } else {
                            claim
                        }
                    }
                )
            }
        }

        updateCurrentDocuments(updatedItems)
        pendingConfirmedSelection = buildSelection(selectedDocumentId)

        coroutineScope.launch {
            presentationRequestInteractor.updateRequestedDocuments(currentDocuments)

            presentationLoadingInteractor.observeResponse().collect { state ->
                when (state) {
                    is PresentationLoadingObserveResponsePartialState.UserAuthenticationRequired -> {
                        handleUserAuthentication(request, state.authenticationData, true)
                    }

                    is PresentationLoadingObserveResponsePartialState.RequestReadyToBeSent -> {
                        navigationService.navigate(ToWeb(PRESENTATION.SCREENS.PRESENTATION_LOADING))

                        when (val result = presentationLoadingInteractor.sendRequestedDocuments()) {
                            is PresentationLoadingSendRequestedDocumentPartialState.Success -> {
                                promotePendingSelection()
                            }
                            is PresentationLoadingSendRequestedDocumentPartialState.Failure -> {
                                emitEvent(createErrorResponse(request, result.error))
                                cleanupPresentation()
                            }
                        }
                    }

                    is PresentationLoadingObserveResponsePartialState.Failure -> {
                        cleanupPresentation()
                        emitEvent(createErrorResponse(request, state.error))
                    }
                    is PresentationLoadingObserveResponsePartialState.Success -> {
                        promotePendingSelection()
                        cleanupPresentation()
                        emitEvent(createSuccessResponse(request, null))
                    }
                    is PresentationLoadingObserveResponsePartialState.Redirect -> {
                        val redirectUri = state.uri.toString()
                        Log.d("PRESENTATION", "Redirect URI: $redirectUri")
                        promotePendingSelection()
                        cleanupPresentation()
                        navigationService.navigate(ToExternal(redirectUri))
                        emitEvent(
                            createSuccessResponse(
                                request,
                                mapOf(
                                    "redirectUrl" to redirectUri
                                )
                            )
                        )
                    }
                }
            }
        }
        return createSuccessResponse(request, null)
    }

    private fun handleGetRequestDocuments(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            try {
                presentationRequestInteractor.getRequestDocuments().collect { response ->
                    when (response) {
                        is PresentationRequestInteractorPartialState.Success -> {
                            val vendorKey = deriveVendorKey(response.verifierName)
                            val rememberedSelection = loadSelection(vendorKey, response.requestDocuments)
                            val (preparedDocuments, savedSelectionApplied) =
                                applyRememberedSelection(response.requestDocuments, rememberedSelection)
                            val quickFlowAvailable = rememberedSelection != null &&
                                preparedDocuments.any { it.docId == rememberedSelection.selectedDocumentId }

                            updateCurrentDocuments(preparedDocuments)

                            val documentsForJson = preparedDocuments.map { doc ->

                                val fieldsForJson = doc.claims.map { claim ->
                                    mapOf(
                                        "id" to claim.id,
                                        "readableName" to claim.displayTitle,
                                        "value" to claim.value,
                                        "checked" to claim.isChecked,
                                        "enabled" to claim.isEnabled,
                                        "elementIdentifier" to claim.elementIdentifier,
                                        "isRequired" to claim.intentToRetain,
//                                        "intentToRetain" to claim.intentToRetain
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
                                    "documentId" to doc.docId,
                                    "title" to doc.docName,
                                    "meta" to meta,
                                    "fields" to fieldsForJson
                                )
                            }

                            val responseData = mapOf(
                                "vendorKey" to vendorKey,
                                "verifierName" to response.verifierName,
                                "verifierIsTrusted" to response.verifierIsTrusted,
                                "quickFlowAvailable" to quickFlowAvailable,
                                "savedSelectionApplied" to savedSelectionApplied,
                                "preferredDocumentId" to (rememberedSelection?.selectedDocumentId),
                                "documents" to documentsForJson,
                            )

                            emitEvent(createSuccessResponse(request, responseData))
                        }

                        is PresentationRequestInteractorPartialState.NoData -> {
                            val vendorKey = deriveVendorKey(response.verifierName)
                            emitEvent(
                                createSuccessResponse(
                                    request, mapOf(
                                        "vendorKey" to vendorKey,
                                        "verifierName" to response.verifierName,
                                        "verifierIsTrusted" to response.verifierIsTrusted,
                                        "quickFlowAvailable" to false,
                                        "savedSelectionApplied" to false,
                                        "preferredDocumentId" to null,
                                        "documents" to emptyList<Map<String, Any>>()
                                    )
                                )
                            )
                        }

                        is PresentationRequestInteractorPartialState.Disconnect -> {
                            cleanupPresentation()
                            emitEvent(createErrorResponse(request, "DISCONNECTED"))
                        }

                        is PresentationRequestInteractorPartialState.Failure -> {
                            cleanupPresentation()
                            emitEvent(createErrorResponse(request, response.error))
                        }
                    }
                    resourceProvider.provideContext()
                        .getSystemService(Vibrator::class.java)
                        ?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))

                }
            } catch (e: Exception) {
                if (e !is org.koin.core.error.ScopeNotCreatedException &&
                    e !is org.koin.core.error.InstanceCreationException) {
                    try {
                        cleanupPresentation()
                    } catch (_: Exception) {}
                }

                emitEvent(createErrorResponse(request, e.message))
            }
        }
        return createSuccessResponse(request, null)
    }

    private fun handleSetVendorPresentationPreference(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid data").also { emitEvent(it) }

        val vendorKey = data["vendorKey"] as? String
            ?: return createErrorResponse(request, "Missing vendorKey").also { emitEvent(it) }

        val remember = data["remember"] as? Boolean
            ?: return createErrorResponse(request, "Missing remember").also { emitEvent(it) }

        val allSelections = loadSelectionsMap()

        if (!remember) {
            val selectedDocumentId = lastSuccessfulSelection?.selectedDocumentId
            if (selectedDocumentId != null) {
                allSelections[vendorKey]?.remove(selectedDocumentId)
                if (allSelections[vendorKey].isNullOrEmpty()) {
                    allSelections.remove(vendorKey)
                }
            } else {
                allSelections.remove(vendorKey)
            }
            saveSelectionsMap(allSelections)
            return createSuccessResponse(request, null).also { emitEvent(it) }
        }

        val successSelection = lastSuccessfulSelection
            ?: return createErrorResponse(request, "No successful selection to save").also { emitEvent(it) }

        val vendorSelections = allSelections.getOrPut(vendorKey) { mutableMapOf() }
        vendorSelections[successSelection.selectedDocumentId] = StoredPresentationSelection(
            selectedFieldIds = successSelection.selectedFieldIds,
            updatedAt = System.currentTimeMillis(),
        )
        saveSelectionsMap(allSelections)
        return createSuccessResponse(request, null).also { emitEvent(it) }
    }

    private fun promotePendingSelection() {
        lastSuccessfulSelection = pendingConfirmedSelection ?: return
    }

    private fun resetPresentationSelectionState() {
        lastSuccessfulSelection = null
        pendingConfirmedSelection = null
    }

    private fun buildSelection(selectedDocumentId: String): RememberedPresentationSelection? {
        val selectedDoc = currentDocuments.firstOrNull { it.docId == selectedDocumentId } ?: return null
        val selectedFieldIds = selectedDoc.claims
            .filter { it.isChecked }
            .map { it.id }
            .toSet()
        return RememberedPresentationSelection(
            selectedDocumentId = selectedDocumentId,
            selectedFieldIds = selectedFieldIds
        )
    }

    private fun deriveVendorKey(verifierName: String?): String {
        val config = PresentationConfigStore.getConfig()
        val mode = config?.mode
        val keySource = when (mode) {
            is lv.zzdats.commonfeature.config.PresentationMode.OpenId4Vp -> {
                val uri = Uri.parse(mode.uri)
                uri.getQueryParameter("client_id")
                    ?: uri.getQueryParameter("request_uri")
                    ?: uri.host
                    ?: verifierName
                    ?: DashboardScreens.Dashboard.screenRoute
            }
            else -> verifierName ?: DashboardScreens.Dashboard.screenRoute
        }
        return "oid4vp:${keySource.trim().lowercase()}"
    }

    private fun applyRememberedSelection(
        documents: List<PresentationDocument>,
        rememberedSelection: RememberedPresentationSelection?
    ): Pair<List<PresentationDocument>, Boolean> {
        if (rememberedSelection == null) return documents to false

        var applied = false
        val updated = documents.map { doc ->
            if (doc.docId != rememberedSelection.selectedDocumentId) {
                doc.copy(claims = doc.claims.map { it.copy(isChecked = false) })
            } else {
                applied = true
                doc.copy(
                    claims = doc.claims.map { claim ->
                        if (claim.isEnabled) {
                            claim.copy(isChecked = rememberedSelection.selectedFieldIds.contains(claim.id))
                        } else {
                            claim
                        }
                    }
                )
            }
        }
        return updated to applied
    }

    private suspend fun loadSelection(
        vendorKey: String,
        requestDocuments: List<PresentationDocument>,
    ): RememberedPresentationSelection? {
        val allSelections = loadSelectionsMap()
        val vendorSelections = allSelections[vendorKey] ?: return null
        val activeDocumentIds = walletCoreDocumentsController.getAllDocuments()
            .map { it.id }
            .toSet()
        val revokedDocumentIds = runCatching { walletCoreDocumentsController.getRevokedDocumentIds().toSet() }
            .getOrDefault(emptySet())
        val requestDocumentIds = requestDocuments.map { it.docId }.toSet()

        var changed = false
        val validSelections = vendorSelections.filterKeys { documentId ->
            val isValid = documentId in activeDocumentIds &&
                documentId !in revokedDocumentIds &&
                documentId.isNotBlank()
            if (!isValid) {
                changed = true
            }
            isValid
        }.toMutableMap()

        if (validSelections.isEmpty()) {
            if (vendorSelections.isNotEmpty()) {
                allSelections.remove(vendorKey)
                saveSelectionsMap(allSelections)
            }
            return null
        }

        if (changed) {
            allSelections[vendorKey] = validSelections
            saveSelectionsMap(allSelections)
        }

        val matchedDocumentId = requestDocuments.firstOrNull { it.docId in validSelections }?.docId
            ?: validSelections
                .filterKeys { it in requestDocumentIds }
                .maxByOrNull { (_, selection) -> selection.updatedAt ?: Long.MIN_VALUE }
                ?.key
            ?: return null

        val matchedSelection = validSelections[matchedDocumentId] ?: return null
        return RememberedPresentationSelection(
            selectedDocumentId = matchedDocumentId,
            selectedFieldIds = matchedSelection.selectedFieldIds,
        )
    }

    private fun loadSelectionsMap(): MutableMap<String, MutableMap<String, StoredPresentationSelection>> {
        val raw = prefKeys.getPresentationVendorSelections()
        if (raw.isBlank()) return mutableMapOf()

        return runCatching {
            val root = JSONObject(raw)
            buildMap<String, MutableMap<String, StoredPresentationSelection>> {
                val vendorKeys = root.keys()
                while (vendorKeys.hasNext()) {
                    val vendorKey = vendorKeys.next()
                    val entry = root.optJSONObject(vendorKey) ?: continue
                    val normalized = parseVendorSelections(entry)
                    if (normalized.isNotEmpty()) {
                        put(vendorKey, normalized.toMutableMap())
                    }
                }
            }.toMutableMap()
        }.getOrDefault(mutableMapOf())
    }

    private fun parseVendorSelections(entry: JSONObject): Map<String, StoredPresentationSelection> {
        val legacyDocumentId = entry.optString("selectedDocumentId")
        if (legacyDocumentId.isNotBlank()) {
            return parseStoredSelection(entry)?.let { selection ->
                mapOf(legacyDocumentId to selection)
            }.orEmpty()
        }

        return buildMap {
            val documentKeys = entry.keys()
            while (documentKeys.hasNext()) {
                val documentId = documentKeys.next()
                if (documentId.isBlank()) continue
                val documentEntry = entry.optJSONObject(documentId) ?: continue
                parseStoredSelection(documentEntry)?.let { put(documentId, it) }
            }
        }
    }

    private fun parseStoredSelection(entry: JSONObject): StoredPresentationSelection? {
        val ids = mutableSetOf<String>()
        val fieldsArray = entry.optJSONArray("selectedFieldIds") ?: JSONArray()
        for (i in 0 until fieldsArray.length()) {
            fieldsArray.optString(i)?.takeIf { it.isNotBlank() }?.let { ids.add(it) }
        }
        if (ids.isEmpty()) return null
        return StoredPresentationSelection(
            selectedFieldIds = ids,
            updatedAt = entry.optLong("updatedAt").takeIf { it > 0L },
        )
    }

    private fun saveSelectionsMap(map: Map<String, Map<String, StoredPresentationSelection>>) {
        val root = JSONObject()
        map.forEach { (vendorKey, selectionsByDocument) ->
            if (selectionsByDocument.isEmpty()) return@forEach
            val vendorEntry = JSONObject()
            selectionsByDocument.forEach { (documentId, selection) ->
                val entry = JSONObject()
                val ids = JSONArray()
                selection.selectedFieldIds.forEach { ids.put(it) }
                entry.put("selectedFieldIds", ids)
                selection.updatedAt?.let { entry.put("updatedAt", it) }
                vendorEntry.put(documentId, entry)
            }
            root.put(vendorKey, vendorEntry)
        }
        prefKeys.setPresentationVendorSelections(root.toString())
    }

    private suspend fun logCanceledPresentationTransaction() {
        val documentsToLog = currentDocuments
            .filter { document -> document.claims.any { it.isChecked } }
            .ifEmpty { currentDocuments }
            .distinctBy { it.docId }

        if (documentsToLog.isEmpty()) return

        val authority = runCatching {
            presentationScope.get<WalletCorePresentationController>().verifierName
        }.getOrNull()

        documentsToLog.forEach { document ->
            val issuedDocument =
                walletCoreDocumentsController.getDocumentById(document.docId) as? IssuedDocument
                    ?: return@forEach

            transactionLogDao.store(
                TransactionLog(
                    documentId = issuedDocument.id,
                    docType = issuedDocument.toDocumentIdentifier().formatType,
                    nameSpace = issuedDocument.name,
                    eventType = TransactionType.DOCUMENT_PRESENTED.name,
                    authority = authority,
                    status = "CANCELLED",
                )
            )
        }
    }

    private fun cleanupPresentation() {
        try {
            val scope = getOrCreatePresentationScope()
            if (!scope.closed) {
                val interactor = scope.getOrNull<PresentationRequestInteractor>()
                interactor?.stopPresentation()
            }
        } catch (_: org.koin.core.error.ClosedScopeException) {} catch (_: Exception) {}

        PresentationConfigStore.clear()

        try {
            getOrCreatePresentationScope().close()
        } catch (_: Exception) {}

        currentDocuments = emptyList()
    }

    @SuppressLint("RestrictedApi")
    private fun handleUserAuthentication(
        request: BridgeRequest,
        authDataList: List<AuthenticationData>,
        notifyOnAuthFailure: Boolean,
        index: Int = 0
    ) {
        if (index >= authDataList.size) {
            presentationLoadingInteractor.sendRequestedDocuments()
            emitEvent(createSuccessResponse(request, null))
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
                notifyOnAuthenticationFailure = notifyOnAuthFailure,
                policy = DeviceAuthenticationPolicy.HARDWARE_KEY,
                resultHandler = DeviceAuthenticationResult(
                    onAuthenticationSuccess = {
                        authData.onAuthenticationSuccess()
                        if (isFinalAuthentication) {
                            when (val result = presentationLoadingInteractor.sendRequestedDocuments()) {
                                is PresentationLoadingSendRequestedDocumentPartialState.Success -> {
                                    promotePendingSelection()
                                    emitEvent(createSuccessResponse(request, null))
                                }
                                is PresentationLoadingSendRequestedDocumentPartialState.Failure -> {
                                    coroutineScope.launch {
                                        cleanupPresentation()
                                        emitEvent(createErrorResponse(request, result.error))
                                    }
                                }
                            }
                        } else {
                            // Add small delay before next auth prompt
                            coroutineScope.launch {
                                delay(500)
                                handleUserAuthentication(
                                    request,
                                    authDataList,
                                    notifyOnAuthFailure,
                                    index + 1
                                )
                            }
                        }
                    },
                    onAuthenticationCancelled = {
                        coroutineScope.launch {
                            logCanceledPresentationTransaction()
                            cleanupPresentation()
                            emitEvent(createErrorResponse(request, "AUTHENTICATION_ERROR"))
                        }
                    },
                    onAuthenticationError = {
                        coroutineScope.launch {
                            cleanupPresentation()
                            emitEvent(createErrorResponse(request, "AUTHENTICATION_ERROR"))
                        }
                    },
                    onAuthenticationFailure = {
                        if (notifyOnAuthFailure) {
                            emitEvent(createErrorResponse(request, "BIOMETRIC_FAILED"))
                        }
                    }
                )
            )
    }
}
