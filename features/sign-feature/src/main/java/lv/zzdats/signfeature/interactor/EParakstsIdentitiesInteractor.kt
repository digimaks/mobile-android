// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.signfeature.interactor

import android.util.Log
import com.upokecenter.cbor.CBORObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import lv.zzdats.businesslogic.util.SecureRandomUtils
import lv.zzdats.commonfeature.util.convertAnyToDateOnly
import lv.zzdats.commonfeature.util.extractValueFromDocumentOrEmpty
import lv.zzdats.commonfeature.features.auth.EparakstsAuthLauncher
import lv.zzdats.corelogic.controller.IssuanceMethod
import lv.zzdats.corelogic.controller.LoadEParakstDocumentState
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.model.DocumentIdentifier
import lv.zzdats.networklogic.api.wallet.DocumentType
import lv.zzdats.networklogic.api.wallet.WalletApiClient
import lv.zzdats.networklogic.model.wallet.EParakstIdentitiesResponse
import lv.zzdats.networklogic.model.wallet.EParakstIdentityResponse
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.signfeature.BuildConfig
import java.io.ByteArrayOutputStream
import java.security.SecureRandom


sealed class EParakstIdentitiesState {
    data object Initial : EParakstIdentitiesState()
    data class RedirectToAuth(val url: String) : EParakstIdentitiesState()
    data class SelectionNeeded(
        val response: EParakstIdentitiesResponse
    ) : EParakstIdentitiesState()
    data class Success(val response: EParakstIdentitiesResponse) : EParakstIdentitiesState()
    data class Failure(val error: String?) : EParakstIdentitiesState()
    data object Complete : EParakstIdentitiesState()
}

interface EParakstIdentitiesInteractor {
    fun initiateEParakstsIdentities(documentType: DocumentType): Flow<EParakstIdentitiesState>
    fun handleIdentitiesResult(token: String)
    fun storeSelectedIdentities(
        selectedIds: List<String>,
        response: EParakstIdentitiesResponse,
        documentType: DocumentType
    )
}

class EParakstIdentitiesInteractorImpl(
    private val walletApiClient: WalletApiClient,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val resourceProvider: ResourceProvider
) : EParakstIdentitiesInteractor {

    private var currentDocumentType: DocumentType? = null
    private var useCrossDeviceFlow: Boolean = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val stateFlow = MutableStateFlow<EParakstIdentitiesState>(EParakstIdentitiesState.Initial)

    override fun initiateEParakstsIdentities(documentType: DocumentType): Flow<EParakstIdentitiesState> {
        stateFlow.value = EParakstIdentitiesState.Initial
        currentDocumentType = documentType
        useCrossDeviceFlow = !EparakstsAuthLauncher.canHandleEparakstsApp(resourceProvider.provideContext())
        coroutineScope.launch {
            walletApiClient.getEParakstIdentitiesUrl(
                redirectUrl = BuildConfig.EPARAKSTS_REDIRECT_URI,
                crossDevice = if (useCrossDeviceFlow) true else null
            )
                .fold(
                    onSuccess = { response ->
                        stateFlow.emit(EParakstIdentitiesState.RedirectToAuth(response.redirectUrl))
                    },
                    onFailure = { error ->
                        stateFlow.emit(EParakstIdentitiesState.Failure(error.message))
                    }
                )
        }

        return stateFlow
    }

    override fun handleIdentitiesResult(token: String) {
        coroutineScope.launch {
            walletApiClient.getEParakstIdentities(
                token = token,
                crossDevice = if (useCrossDeviceFlow) true else null
            )
                .fold(
                    onSuccess = { response ->
                        val identities = when(currentDocumentType) {
                            DocumentType.ESEAL -> response.eSeal
                            DocumentType.ESIGN -> response.eSign
                            else -> emptyList()
                        }

                        if (identities.isEmpty()) {
                            val errorCode = when(currentDocumentType) {
                                DocumentType.ESEAL -> "sign_not_found_eseal_signing_identity"
                                DocumentType.ESIGN -> "sign_not_found_signing_identity"
                                else -> "sign_not_found_identity"
                            }
                            stateFlow.emit(EParakstIdentitiesState.Failure(errorCode))
                        } else if (identities.size > 1) {
                            stateFlow.emit(EParakstIdentitiesState.SelectionNeeded(response))
                        } else {
                            currentDocumentType?.let {
                                val documentIds = storeEParakstIdentities(response, it)

                                if (documentIds.isEmpty()) {
                                    stateFlow.emit(EParakstIdentitiesState.Failure("sign_documents_already_exist"))
                                } else {
                                    stateFlow.emit(EParakstIdentitiesState.Success(response))
                                    stateFlow.emit(EParakstIdentitiesState.Complete)
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        stateFlow.emit(EParakstIdentitiesState.Failure(error.message))
                    }
                )
        }
    }

    override fun storeSelectedIdentities(
        selectedIds: List<String>,
        response: EParakstIdentitiesResponse,
        documentType: DocumentType
    ) {
        currentDocumentType = documentType
        coroutineScope.launch {
            // Only process identities matching requested type
            val filteredResponse = EParakstIdentitiesResponse(
                eSeal = if(documentType == DocumentType.ESEAL)
                    response.eSeal.filter { identity -> selectedIds.contains(identity.Sid) }
                else emptyList(),
                eSign = if(documentType == DocumentType.ESIGN)
                    response.eSign.filter { identity -> selectedIds.contains(identity.Sid) }
                else emptyList()
            )

            val documentIds = storeEParakstIdentities(filteredResponse, documentType)

            if (documentIds.isEmpty()) {
                val hadDocumentsToProcess = when(documentType) {
                    DocumentType.ESEAL -> filteredResponse.eSeal.isNotEmpty()
                    DocumentType.ESIGN -> filteredResponse.eSign.isNotEmpty()
                    else -> false
                }

                if (hadDocumentsToProcess) {
                    stateFlow.emit(EParakstIdentitiesState.Failure("sign_documents_already_exist"))
                } else {
                    stateFlow.emit(EParakstIdentitiesState.Failure("sign_no_documents_selected"))
                }
            } else {
                stateFlow.emit(EParakstIdentitiesState.Complete)
            }
        }
    }


    private suspend fun storeEParakstIdentities(
        response: EParakstIdentitiesResponse,
        documentType: DocumentType
    ): List<String> {
        val documentIds = mutableListOf<String>()

        when(documentType) {
            DocumentType.ESIGN -> {
                response.eSign.forEach { identity ->
                    val sampleData = createEParakstSampleData(identity, isESeal = false)
                    walletCoreDocumentsController.loadEParakstDocument(
                        sampleData = sampleData,
                        docType = DocumentIdentifier.MdocESign.formatType,
                        nameSpace = "eSign",
                        issuanceMethod = IssuanceMethod.EPARAKSTS
                    ).collect { state ->
                        when(state) {
                            is LoadEParakstDocumentState.Success -> documentIds.add(state.documentId)
                            is LoadEParakstDocumentState.Failure -> Log.e("EParakst", "Failed to store eSign: ${state.error}")
                        }
                    }
                }
            }
            DocumentType.ESEAL -> {
                val docs = walletCoreDocumentsController.getAllDocumentsByType(listOf(DocumentIdentifier.MdocESeal))
                val existingSids = docs.map { doc ->
                    extractValueFromDocumentOrEmpty(doc, "sid")
                }.toSet()

                var totalAttempted = 0

                response.eSeal.forEach { identity ->
                    totalAttempted++
                    if (!existingSids.contains(identity.Sid)) {
                        val sampleData = createEParakstSampleData(identity, isESeal = true)
                        walletCoreDocumentsController.loadEParakstDocument(
                            sampleData = sampleData,
                            docType = DocumentIdentifier.MdocESeal.formatType,
                            nameSpace = "eSeal",
                            issuanceMethod = IssuanceMethod.EPARAKSTS
                        ).collect { state ->
                            when(state) {
                                is LoadEParakstDocumentState.Success -> documentIds.add(state.documentId)
                                is LoadEParakstDocumentState.Failure -> Log.e("EParakst", "Failed to store eSeal: ${state.error}")
                            }
                        }
                    }
                }
            }
            else -> {
                Log.e("EParakst", "Unsupported document type: $documentType")
            }
        }

        return documentIds
    }

    private fun createEParakstSampleData(identity: EParakstIdentityResponse, isESeal: Boolean): ByteArray {
        val random = SecureRandomUtils.create()
        val rootObj = CBORObject.NewMap()

        rootObj["status"] = CBORObject.FromObject("")
        rootObj["version"] = CBORObject.FromObject("1.0")

        val documents = CBORObject.NewArray()

        val docObj = CBORObject.NewMap()
        docObj["docType"] = CBORObject.FromObject(
            if (isESeal) "eu.digimaks.eseal"
            else "eu.digimaks.esign"
        )

        val issuerSigned = CBORObject.NewMap()
        val nameSpaces = CBORObject.NewMap()
        val namespaceArray = CBORObject.NewArray()

        // Add each element as a properly tagged issuer signed item
        addIssuerSignedItem(namespaceArray, "sid", identity.Sid, random)
        addIssuerSignedItem(namespaceArray, "cn", identity.cn, random)
        addIssuerSignedItem(namespaceArray, "expiresOn", formatDateOnly(identity.ExpiresOn), random)
        addIssuerSignedItem(namespaceArray, "issuedOn", formatDateOnly(identity.IssuedOn), random)
        addIssuerSignedItem(namespaceArray, "type", if (isESeal) "eSeal" else "eSign", random)

        // Add namespace to nameSpaces
        nameSpaces[docObj["docType"]] = namespaceArray
        issuerSigned["nameSpaces"] = nameSpaces
        docObj["issuerSigned"] = issuerSigned
        documents.Add(docObj)
        rootObj["documents"] = documents

        // Convert to byte array
        val outputStream = ByteArrayOutputStream()
        CBORObject.Write(rootObj, outputStream)
        return outputStream.toByteArray()
    }

    private fun addIssuerSignedItem(array: CBORObject, elementIdentifier: String, elementValue: String, random: SecureRandom) {
        // Create the inner issuer signed item
        val issuerSignedItem = CBORObject.NewMap()
        issuerSignedItem["digestID"] = CBORObject.FromObject(1)
        issuerSignedItem["random"] = CBORObject.FromObject(ByteArray(16).apply { random.nextBytes(this) })
        issuerSignedItem["elementIdentifier"] = CBORObject.FromObject(elementIdentifier)
        issuerSignedItem["elementValue"] = CBORObject.FromObject(elementValue)

        // Encode the issuer signed item as bytes
        val issuerSignedItemBytes = issuerSignedItem.EncodeToBytes()

        // Create a tagged byte string with tag 24 (expected by the wallet sdk)
        val taggedItem = CBORObject.FromObjectAndTag(issuerSignedItemBytes, 24)

        array.Add(taggedItem)
    }

    private fun formatDateOnly(value: String): String {
        return convertAnyToDateOnly(value) ?: value
    }
}
