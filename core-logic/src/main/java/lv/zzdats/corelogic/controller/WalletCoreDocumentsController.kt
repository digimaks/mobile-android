// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.controller

import android.util.Log
import eu.europa.ec.eudi.openid4vci.CredentialIssuanceError
import eu.europa.ec.eudi.openid4vci.MsoMdocCredential
import eu.europa.ec.eudi.openid4vci.SdJwtVcCredential
import eu.europa.ec.eudi.wallet.EudiWallet
import eu.europa.ec.eudi.wallet.document.DeferredDocument
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultCreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultKeyUnlockData
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import eu.europa.ec.eudi.wallet.document.format.MsoMdocFormat
import eu.europa.ec.eudi.wallet.document.format.SdJwtVcFormat
import eu.europa.ec.eudi.wallet.issue.openid4vci.DeferredIssueResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.IssueEvent
import eu.europa.ec.eudi.wallet.issue.openid4vci.Offer
import eu.europa.ec.eudi.wallet.issue.openid4vci.OfferResult
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationResult
import lv.zzdats.authlogic.model.BiometricCrypto
import lv.zzdats.businesslogic.extensions.compareLocaleLanguage
import lv.zzdats.businesslogic.extensions.safeAsync
import lv.zzdats.businesslogic.security.SecurityProviderGuard
import lv.zzdats.corelogic.config.WalletConfig
import lv.zzdats.corelogic.extension.documentIdentifier
import lv.zzdats.corelogic.model.DeferredDocumentData
import lv.zzdats.corelogic.model.DocumentIdentifier
import lv.zzdats.corelogic.model.FormatType
import lv.zzdats.corelogic.model.ScopedDocument
import lv.zzdats.corelogic.model.toDocumentIdentifier
import lv.zzdats.networklogic.error.ErrorUtils
import lv.zzdats.resourceslogic.R
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.storagelogic.dao.BookmarkDao
import lv.zzdats.storagelogic.dao.DocumentIssuanceMethodDao
import lv.zzdats.storagelogic.dao.TransactionLogDao
import lv.zzdats.storagelogic.model.Bookmark
import lv.zzdats.storagelogic.model.DocumentIssuanceMethod
import lv.zzdats.storagelogic.model.TransactionLog
import lv.zzdats.storagelogic.model.TransactionType
import java.util.Locale
import eu.europa.ec.eudi.statium.Status
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import lv.zzdats.corelogic.extension.getLocalizedDisplayName
import lv.zzdats.storagelogic.dao.RevokedDocumentDao
import org.multipaz.securearea.AndroidKeystoreSecureArea
import org.multipaz.securearea.CreateKeySettings
import kotlin.time.ExperimentalTime


enum class IssuanceMethod(val value: String) {
    SMART_ID("smart_id"),
    EPARAKSTS("eparaksts"),
    QR("qr");

    companion object {
        fun fromValue(value: String?): IssuanceMethod? =
            entries.firstOrNull { it.value == value }
    }
}

sealed class IssueDocumentPartialState {
    data class Success(val documentId: String) : IssueDocumentPartialState()
    data class DeferredSuccess(val deferredDocuments: Map<String, String>) :
        IssueDocumentPartialState()

    data class Failure(
        val errorMessage: String,
        val cause: Throwable? = null
    ) : IssueDocumentPartialState()
    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult,
    ) : IssueDocumentPartialState()
}

sealed class IssueDocumentsPartialState {
    data class Success(val documentIds: List<String>) : IssueDocumentsPartialState()
    data class DeferredSuccess(val deferredDocuments: Map<String, String>) :
        IssueDocumentsPartialState()

    data class PartialSuccess(
        val documentIds: List<String>,
        val nonIssuedDocuments: Map<String, String>,
    ) : IssueDocumentsPartialState()

    data class Failure(
        val errorMessage: String,
        val errorCode: String? = null,
        val cause: Throwable? = null
    ) : IssueDocumentsPartialState()
    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult,
    ) : IssueDocumentsPartialState()
}

sealed class LoadEParakstDocumentState {
    data class Success(val documentId: String) : LoadEParakstDocumentState()
    data class Failure(val error: String?) : LoadEParakstDocumentState()
}

sealed class AddSampleDataPartialState {
    data object Success : AddSampleDataPartialState()
    data class Failure(val error: String) : AddSampleDataPartialState()
}

sealed class DeleteDocumentPartialState {
    data object Success : DeleteDocumentPartialState()
    data class Failure(val errorMessage: String) : DeleteDocumentPartialState()
}

sealed class DeleteAllDocumentsPartialState {
    data object Success : DeleteAllDocumentsPartialState()
    data class Failure(val errorMessage: String) : DeleteAllDocumentsPartialState()
}

sealed class ResolveDocumentOfferPartialState {
    data class Success(val offer: Offer) : ResolveDocumentOfferPartialState()
    data class Failure(val errorMessage: String) : ResolveDocumentOfferPartialState()
}

sealed class FetchScopedDocumentsPartialState {
    data class Success(val documents: List<ScopedDocument>) : FetchScopedDocumentsPartialState()
    data class Failure(val errorMessage: String) : FetchScopedDocumentsPartialState()
}

sealed class IssueDeferredDocumentPartialState {
    data class Issued(
        val deferredDocumentData: DeferredDocumentData,
    ) : IssueDeferredDocumentPartialState()

    data class NotReady(
        val deferredDocumentData: DeferredDocumentData,
    ) : IssueDeferredDocumentPartialState()

    data class Failed(
        val documentId: DocumentId,
        val errorMessage: String,
    ) : IssueDeferredDocumentPartialState()

    data class Expired(
        val documentId: DocumentId,
    ) : IssueDeferredDocumentPartialState()
}

interface WalletCoreDocumentsController {

    /**
     * @return All the documents from the Database.
     * */

//    fun loadSampleData(sampleDataByteArray: ByteArray): Flow<LoadSampleDataPartialState>

    fun loadEParakstDocument(
        sampleData: ByteArray,
        docType: String,
        nameSpace: String,
        issuanceMethod: IssuanceMethod = IssuanceMethod.EPARAKSTS
    ): Flow<LoadEParakstDocumentState>

    /**
     * Adds the sample data into the Database.
     * */
    fun addSampleData(): Flow<AddSampleDataPartialState>

    fun getAllDocuments(): List<Document>

    fun getAllIssuedDocuments(): List<IssuedDocument>

    fun getAllDocumentsByType(documentIdentifiers: List<DocumentIdentifier>): List<IssuedDocument>

    fun getDocumentById(documentId: DocumentId): Document?

    fun getMainPidDocument(): IssuedDocument?

    fun issueDocument(
        issuanceMethod: IssuanceMethod,
        configId: String,
    ): Flow<IssueDocumentPartialState>

    fun issueDocumentsByOfferUri(
        offerUri: String,
        txCode: String? = null,
        issuanceMethod: IssuanceMethod,
    ): Flow<IssueDocumentsPartialState>

    fun reIssueDocument(
        documentId: DocumentId,
        issuerId: String,
        allowAuthorizationFallback: Boolean,
    ): Flow<IssueDocumentsPartialState>

    fun deleteDocument(
        documentId: String,
    ): Flow<DeleteDocumentPartialState>

    fun deleteAllDocuments(mainPidDocumentId: String): Flow<DeleteAllDocumentsPartialState>

    fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferPartialState>

    fun issueDeferredDocument(docId: DocumentId): Flow<IssueDeferredDocumentPartialState>

    fun resumeOpenId4VciWithAuthorization(uri: String)

    suspend fun getScopedDocuments(locale: Locale): FetchScopedDocumentsPartialState

    suspend fun getRevokedDocumentIds(): List<String>

    suspend fun isDocumentRevoked(id: String): Boolean

    suspend fun getDocumentIssuanceMethod(documentId: DocumentId): IssuanceMethod?

    suspend fun resolveDocumentStatus(document: IssuedDocument): Result<Status>
}

class WalletCoreDocumentsControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val eudiWallet: EudiWallet,
    private val walletConfig: WalletConfig,
    private val transactionLogDao: TransactionLogDao,
    private val bookmarkDao: BookmarkDao,
    private val documentIssuanceMethodDao: DocumentIssuanceMethodDao,
    private val revokedDocumentDao: RevokedDocumentDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : WalletCoreDocumentsController {

    private val genericErrorMessage
        get() = resourceProvider.genericErrorMessage()

    private val documentErrorMessage
        get() = resourceProvider.getString(R.string.issuance_generic_error)

    private val openId4VciManager by lazy {
        SecurityProviderGuard.harden("before_openid4vci_manager")
        eudiWallet.createOpenId4VciManager()
    }

    private suspend fun logTransaction(
        documentId: String,
        docType: String,
        nameSpace: String,
        type: TransactionType,
        status: String = "SUCCESS",
        authority: String? = null,
    ) {
        transactionLogDao.store(
            TransactionLog(
                documentId = documentId,
                docType = docType,
                nameSpace = nameSpace,
                status = status,
                eventType = type.name,
                authority = authority,
            )
        )
    }

    @OptIn(ExperimentalTime::class)
    override fun loadEParakstDocument(
        sampleData: ByteArray,
        docType: String,
        nameSpace: String,
        issuanceMethod: IssuanceMethod
    ): Flow<LoadEParakstDocumentState> = flow {
        eudiWallet.loadMdocSampleDocuments(
            sampleData = sampleData,
            createSettings = CreateDocumentSettings(
                secureAreaIdentifier = AndroidKeystoreSecureArea.IDENTIFIER,
                createKeySettings = CreateKeySettings(),
                credentialPolicy = CredentialPolicy.RotateUse,
                numberOfCredentials = 1
            ),
            documentNamesMap = mapOf(docType to nameSpace)
        ).kotlinResult
            .onSuccess { documents ->
                documents.firstOrNull()?.let { document ->
                    val documentId = document

                    bookmarkDao.store(Bookmark(identifier = documentId))
                    persistIssuanceMethod(documentId, issuanceMethod)

                    logTransaction(
                        documentId = documentId,
                        docType = docType,
                        nameSpace = nameSpace,
                        type = TransactionType.DOCUMENT_ISSUED
                    )

                    emit(LoadEParakstDocumentState.Success(documentId))
                } ?: emit(LoadEParakstDocumentState.Failure(null))
            }
            .onFailure {
                emit(LoadEParakstDocumentState.Failure(it.message ?: genericErrorMessage))
            }
    }.safeAsync {
        LoadEParakstDocumentState.Failure(it.localizedMessage ?: genericErrorMessage)
    }

    override fun addSampleData(): Flow<AddSampleDataPartialState> = flow {
        emit(AddSampleDataPartialState.Success)
    }.safeAsync {
        AddSampleDataPartialState.Failure(it.localizedMessage ?: genericErrorMessage)
    }

    override fun getAllDocuments(): List<Document> =
        eudiWallet.getDocuments { it is IssuedDocument || it is DeferredDocument }

    override fun getAllIssuedDocuments(): List<IssuedDocument> =
        eudiWallet.getDocuments().filterIsInstance<IssuedDocument>()

    override suspend fun getScopedDocuments(locale: Locale): FetchScopedDocumentsPartialState {
        return try {

            val metadata = openId4VciManager.getIssuerMetadata().getOrThrow()

            val documents = metadata.credentialConfigurationsSupported.map { (id, config) ->

                val name: String = config.credentialMetadata.getLocalizedDisplayName(
                    userLocale = locale,
                    fallback = id.value
                )

                val isPid: Boolean = when (config) {
                    is MsoMdocCredential -> config.docType.toDocumentIdentifier() == DocumentIdentifier.MdocPid
                    is SdJwtVcCredential -> config.type.toDocumentIdentifier() == DocumentIdentifier.SdJwtPid
                    else -> false
                }

                ScopedDocument(
                    name = name,
                    configurationId = id.value,
                    isPid = isPid
                )
            }
            if (documents.isNotEmpty()) {
                FetchScopedDocumentsPartialState.Success(documents)
            } else {
                FetchScopedDocumentsPartialState.Failure(genericErrorMessage)
            }
        } catch (e: Exception) {
            FetchScopedDocumentsPartialState.Failure(e.localizedMessage ?: genericErrorMessage)
        }
    }

    override fun getAllDocumentsByType(documentIdentifiers: List<DocumentIdentifier>): List<IssuedDocument> =
        getAllDocuments()
            .filterIsInstance<IssuedDocument>()
            .filter {
                when (it.format) {
                    is MsoMdocFormat -> documentIdentifiers.any { id ->
                        id.formatType == (it.format as MsoMdocFormat).docType
                    }

                    is SdJwtVcFormat -> documentIdentifiers.any { id ->
                        id.formatType == (it.format as SdJwtVcFormat).vct
                    }
                }
            }

    override fun getDocumentById(documentId: DocumentId): Document? {
        return eudiWallet.getDocumentById(documentId = documentId)
    }

    override fun getMainPidDocument(): IssuedDocument? =
        getAllDocumentsByType(
            documentIdentifiers = listOf(
                DocumentIdentifier.MdocPid,
                DocumentIdentifier.SdJwtPid
            )
        ).minByOrNull { it.createdAt }

    override fun issueDocument(
        issuanceMethod: IssuanceMethod,
        configId: String,
    ): Flow<IssueDocumentPartialState> = flow {
        issueDocumentWithOpenId4VCI(
            configId = configId,
            issuanceMethod = issuanceMethod
        ).collect { response ->
            when (response) {
                is IssueDocumentsPartialState.Failure -> emit(
                    IssueDocumentPartialState.Failure(
                        errorMessage = documentErrorMessage,
                        cause = response.cause
                    )
                )

                is IssueDocumentsPartialState.Success -> {
                    response.documentIds.forEach { docId ->
                        getDocumentById(docId)?.let { doc ->
                            val authority = try {
                                // TODO: Fix
                                doc.issuerMetadata?.claims?.firstOrNull { it.display.firstOrNull()?.name == "issuing_authority" }?.display?.firstOrNull()?.name
                            } catch (e: Exception) {
                                null
                            }

                            logTransaction(
                                documentId = doc.id,
                                docType = doc.toDocumentIdentifier().formatType,
                                nameSpace = doc.name,
                                type = TransactionType.DOCUMENT_ISSUED,
                                authority = authority,
                            )
                        }
                    }
                    emit(IssueDocumentPartialState.Success(response.documentIds.first()))
                }

                is IssueDocumentsPartialState.UserAuthRequired -> emit(
                    IssueDocumentPartialState.UserAuthRequired(
                        crypto = response.crypto,
                        resultHandler = response.resultHandler
                    )
                )

                is IssueDocumentsPartialState.PartialSuccess -> emit(
                    IssueDocumentPartialState.Success(
                        response.documentIds.first()
                    )
                )

                is IssueDocumentsPartialState.DeferredSuccess -> emit(
                    IssueDocumentPartialState.DeferredSuccess(
                        response.deferredDocuments
                    )
                )
            }
        }
    }.safeAsync {
        IssueDocumentPartialState.Failure(
            errorMessage = documentErrorMessage,
            cause = it
        )
    }

    override fun issueDocumentsByOfferUri(
        offerUri: String,
        txCode: String?,
        issuanceMethod: IssuanceMethod,
    ): Flow<IssueDocumentsPartialState> =
        callbackFlow {
            val onIssueEvent = issuanceCallback(issuanceMethod)
            openId4VciManager.issueDocumentByOfferUri(
                offerUri = offerUri,
                onIssueEvent = onIssueEvent,
                txCode = txCode,
            )
            awaitClose()
        }.safeAsync {
            val mappedError = mapIssuanceError(it)
            IssueDocumentsPartialState.Failure(
                errorMessage = mappedError.message,
                errorCode = mappedError.code,
                cause = mappedError.cause
            )
        }

    override fun reIssueDocument(
        documentId: DocumentId,
        issuerId: String,
        allowAuthorizationFallback: Boolean,
    ): Flow<IssueDocumentsPartialState> =
        callbackFlow {
            val issuanceMethod = getDocumentIssuanceMethod(documentId)
            openId4VciManager.reissueDocument(
                documentId = documentId,
                allowAuthorizationFallback = allowAuthorizationFallback,
                onIssueEvent = issuanceCallback(issuanceMethod)
            )
            awaitClose()
        }.safeAsync {
            val mappedError = mapIssuanceError(it)
            IssueDocumentsPartialState.Failure(
                errorMessage = mappedError.message,
                errorCode = mappedError.code,
                cause = mappedError.cause
            )
        }

    override fun deleteDocument(documentId: String): Flow<DeleteDocumentPartialState> = flow {
        val document = getDocumentById(documentId)

        if (document == null) {
            documentIssuanceMethodDao.delete(documentId)
            emit(DeleteDocumentPartialState.Success)
            return@flow
        }

        eudiWallet.deleteDocumentById(documentId = documentId)
            .kotlinResult
            .onSuccess {
                documentIssuanceMethodDao.delete(documentId)
                document.let { doc ->
                    logTransaction(
                        documentId = doc.id,
                        docType = doc.toDocumentIdentifier().formatType,
                        nameSpace = doc.name,
                        type = TransactionType.DOCUMENT_DELETED,
                    )
                }
                emit(DeleteDocumentPartialState.Success)
            }
            .onFailure {
                emit(
                    DeleteDocumentPartialState.Failure(
                        errorMessage = it.localizedMessage ?: genericErrorMessage
                    )
                )
            }
    }.safeAsync {
        DeleteDocumentPartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMessage
        )
    }

    override fun deleteAllDocuments(mainPidDocumentId: String): Flow<DeleteAllDocumentsPartialState> =
        flow {
            val allDocuments = getAllDocuments()
            val mainPidDocument = getMainPidDocument()

            mainPidDocument?.let {
                val restOfDocuments = allDocuments.minusElement(it)

                var restOfAllDocsDeleted = true
                var restOfAllDocsDeletedFailureReason = ""

                restOfDocuments.forEach { document ->

                    deleteDocument(
                        documentId = document.id
                    ).collect { deleteDocumentPartialState ->
                        when (deleteDocumentPartialState) {
                            is DeleteDocumentPartialState.Failure -> {
                                restOfAllDocsDeleted = false
                                restOfAllDocsDeletedFailureReason =
                                    deleteDocumentPartialState.errorMessage
                            }

                            is DeleteDocumentPartialState.Success -> {}
                        }
                    }
                }

                if (restOfAllDocsDeleted) {
                    deleteDocument(
                        documentId = mainPidDocumentId
                    ).collect { deleteMainPidDocumentPartialState ->
                        when (deleteMainPidDocumentPartialState) {
                            is DeleteDocumentPartialState.Failure -> emit(
                                DeleteAllDocumentsPartialState.Failure(
                                    errorMessage = deleteMainPidDocumentPartialState.errorMessage
                                )
                            )

                            is DeleteDocumentPartialState.Success -> emit(
                                DeleteAllDocumentsPartialState.Success
                            )
                        }
                    }
                } else {
                    emit(DeleteAllDocumentsPartialState.Failure(errorMessage = restOfAllDocsDeletedFailureReason))
                }
            } ?: emit(
                DeleteAllDocumentsPartialState.Failure(
                    errorMessage = genericErrorMessage
                )
            )
        }.safeAsync {
            DeleteAllDocumentsPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun resolveDocumentOffer(offerUri: String): Flow<ResolveDocumentOfferPartialState> =
        callbackFlow {
            openId4VciManager.resolveDocumentOffer(
                offerUri = offerUri,
                onResolvedOffer = { offerResult ->
                    when (offerResult) {
                        is OfferResult.Failure -> {
                            Log.d("LOg", "error: ${offerResult.cause}")
                            trySendBlocking(
                                ResolveDocumentOfferPartialState.Failure(
                                    errorMessage = offerResult.cause.localizedMessage
                                        ?: genericErrorMessage
                                )
                            )
                        }

                        is OfferResult.Success -> {
                            trySendBlocking(
                                ResolveDocumentOfferPartialState.Success(
                                    offer = offerResult.offer
                                )
                            )
                        }
                    }
                }
            )

            awaitClose()
        }.safeAsync {
            Log.d("LOg", "error: ${it.cause}")

            ResolveDocumentOfferPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun issueDeferredDocument(docId: DocumentId): Flow<IssueDeferredDocumentPartialState> =
        callbackFlow {
            (getDocumentById(docId) as? DeferredDocument)?.let { deferredDoc ->
                openId4VciManager.issueDeferredDocument(
                    deferredDocument = deferredDoc,
                    executor = null,
                    onIssueResult = { deferredIssuanceResult ->
                        when (deferredIssuanceResult) {
                            is DeferredIssueResult.DocumentFailed -> {
                                trySendBlocking(
                                    IssueDeferredDocumentPartialState.Failed(
                                        documentId = deferredIssuanceResult.documentId,
                                        errorMessage = deferredIssuanceResult.cause.localizedMessage
                                            ?: documentErrorMessage
                                    )
                                )
                            }

                            is DeferredIssueResult.DocumentIssued -> {
                                trySendBlocking(
                                    IssueDeferredDocumentPartialState.Issued(
                                        DeferredDocumentData(
                                            documentId = deferredIssuanceResult.documentId,
                                            formatType = deferredIssuanceResult.docType,
                                            docName = deferredIssuanceResult.name
                                        )
                                    )
                                )
                            }

                            is DeferredIssueResult.DocumentNotReady -> {
                                trySendBlocking(
                                    IssueDeferredDocumentPartialState.NotReady(
                                        DeferredDocumentData(
                                            documentId = deferredIssuanceResult.documentId,
                                            formatType = deferredIssuanceResult.docType,
                                            docName = deferredIssuanceResult.name
                                        )
                                    )
                                )
                            }

                            is DeferredIssueResult.DocumentExpired -> {
                                trySendBlocking(
                                    IssueDeferredDocumentPartialState.Expired(
                                        documentId = deferredIssuanceResult.documentId
                                    )
                                )
                            }
                        }
                    }
                )
            } ?: trySendBlocking(
                IssueDeferredDocumentPartialState.Failed(
                    documentId = docId,
                    errorMessage = documentErrorMessage
                )
            )

            awaitClose()
        }.safeAsync {
            IssueDeferredDocumentPartialState.Failed(
                documentId = docId,
                errorMessage = it.localizedMessage ?: genericErrorMessage
            )
        }

    override fun resumeOpenId4VciWithAuthorization(uri: String) {
        openId4VciManager.resumeWithAuthorization(uri)
    }

    private fun issueDocumentWithOpenId4VCI(
        configId: String,
        issuanceMethod: IssuanceMethod,
    ): Flow<IssueDocumentsPartialState> =
        callbackFlow {

            val onIssueEvent = issuanceCallback(issuanceMethod)
            openId4VciManager.issueDocumentByConfigurationIdentifiers(
                credentialConfigurationIds = listOf(configId),
                onIssueEvent = onIssueEvent
            )

            awaitClose()

        }.safeAsync {
            val mappedError = mapIssuanceError(it)
            IssueDocumentsPartialState.Failure(
                errorMessage = mappedError.message,
                errorCode = mappedError.code,
                cause = mappedError.cause
            )
        }

    private fun handleDocumentIssued(
        event: IssueEvent.DocumentIssued,
        issuanceMethod: IssuanceMethod?,
    ): Flow<Unit> = flow {
        bookmarkDao.store(Bookmark(identifier = event.documentId))
        issuanceMethod?.let { persistIssuanceMethod(event.documentId, it) }
        val doc = getDocumentById(event.documentId)
        val authority = if (doc is IssuedDocument) extractIssuingAuthority(doc) else null
        logTransaction(
            documentId = event.documentId,
            docType = event.docType.toString(),
            nameSpace = event.name,
            type = TransactionType.DOCUMENT_ISSUED,
            authority = authority
        )
        emit(Unit)
    }

    private fun ProducerScope<IssueDocumentsPartialState>.issuanceCallback(
        issuanceMethod: IssuanceMethod?,
    ): OpenId4VciManager.OnIssueEvent {

        var totalDocumentsToBeIssued = 0
        val nonIssuedDocuments: MutableMap<FormatType, String> = mutableMapOf()
        val deferredDocuments: MutableMap<DocumentId, FormatType> = mutableMapOf()
        val issuedDocuments: MutableMap<DocumentId, FormatType> = mutableMapOf()

        val listener = OpenId4VciManager.OnIssueEvent { event ->
            when (event) {
                is IssueEvent.DocumentFailed -> {
                    nonIssuedDocuments[event.docType] = event.name
                }

                is IssueEvent.DocumentRequiresCreateSettings -> {
                    launch {
                        val offeredDocIdentifier = event.offeredDocument.documentIdentifier

                        val documentIssuanceRule = walletConfig
                            .documentIssuanceConfig
                            .getRuleForDocument(documentIdentifier = offeredDocIdentifier)

                        event.resume(
                            eudiWallet.getDefaultCreateDocumentSettings(
                                offeredDocument = event.offeredDocument,
                                credentialPolicy = documentIssuanceRule.policy,
                                numberOfCredentials = documentIssuanceRule.numberOfCredentials,
                            )
                        )
                    }
                }

                is IssueEvent.DocumentRequiresUserAuth -> {
                    launch {
                        val keyUnlockDataMap =
                            event.keysRequireAuth.mapValues { (keyAlias, secureArea) ->
                                getDefaultKeyUnlockData(secureArea, keyAlias)
                            }

                        val keyUnlockData =
                            keyUnlockDataMap.values.first() //TODO: Revisit this once Core adds support.

                        val cryptoObject = keyUnlockData?.getCryptoObjectForSigning()

                        trySendBlocking(
                            IssueDocumentsPartialState.UserAuthRequired(
                                crypto = BiometricCrypto(cryptoObject),
                                resultHandler = DeviceAuthenticationResult(
                                    onAuthenticationSuccess = { event.resume(keyUnlockDataMap) },
                                    onAuthenticationError = { event.cancel(null) }
                                )
                            )
                        )
                    }
                }

                is IssueEvent.Failure -> {
                    val mappedError = mapIssuanceError(event.cause)
                    trySendBlocking(
                        IssueDocumentsPartialState.Failure(
                            errorMessage = mappedError.message,
                            errorCode = mappedError.code,
                            cause = mappedError.cause
                        )
                    )
                }

                is IssueEvent.Finished -> {

                    if (deferredDocuments.isNotEmpty()) {
                        trySendBlocking(IssueDocumentsPartialState.DeferredSuccess(deferredDocuments))
                        return@OnIssueEvent
                    }

                    if (event.issuedDocuments.isEmpty()) {
                        trySendBlocking(
                            IssueDocumentsPartialState.Failure(
                                errorMessage = documentErrorMessage
                            )
                        )
                        return@OnIssueEvent
                    }

                    if (event.issuedDocuments.size == totalDocumentsToBeIssued) {
                        trySendBlocking(
                            IssueDocumentsPartialState.Success(
                                documentIds = event.issuedDocuments
                            )
                        )
                        return@OnIssueEvent
                    }

                    trySendBlocking(
                        IssueDocumentsPartialState.PartialSuccess(
                            documentIds = event.issuedDocuments,
                            nonIssuedDocuments = nonIssuedDocuments
                        )
                    )
                }

                is IssueEvent.DocumentIssued -> {
                    issuedDocuments[event.documentId] = event.docType
                    // TODO: For now automatically favorite all documents
                    runBlocking {
                        handleDocumentIssued(event, issuanceMethod).collect {}
                    }
                }

                is IssueEvent.Started -> {
                    totalDocumentsToBeIssued = event.total
                }

                is IssueEvent.DocumentDeferred -> {
                    deferredDocuments[event.documentId] = event.docType
                    issuanceMethod?.let { method ->
                        runBlocking {
                            persistIssuanceMethod(event.documentId, method)
                        }
                    }
                }
            }
        }

        return listener
    }

    override suspend fun getRevokedDocumentIds(): List<String> =
        revokedDocumentDao.retrieveAll().map { it.identifier }

    override suspend fun isDocumentRevoked(id: String): Boolean =
        revokedDocumentDao.retrieve(id) != null

    override suspend fun getDocumentIssuanceMethod(documentId: DocumentId): IssuanceMethod? =
        IssuanceMethod.fromValue(documentIssuanceMethodDao.retrieve(documentId)?.issuanceMethod)

    override suspend fun resolveDocumentStatus(document: IssuedDocument): Result<Status> =
        eudiWallet.resolveStatus(document)

    private data class IssuanceError(
        val message: String,
        val code: String?,
        val cause: Throwable? = null
    )

    private fun mapIssuanceError(cause: Throwable?): IssuanceError {
        val issuanceError = findIssuanceError(cause)
        val mapped = when (issuanceError) {
            is CredentialIssuanceError.AccessTokenRequestFailed ->
                mapOAuthError(issuanceError.error, issuanceError.errorDescription)
            is CredentialIssuanceError.IssuanceRequestFailed ->
                mapOAuthError(issuanceError.error, issuanceError.errorDescription)
            else -> null
        }
        return mapped?.copy(cause = cause) ?: IssuanceError(documentErrorMessage, null, cause)
    }

    private fun findIssuanceError(cause: Throwable?): CredentialIssuanceError? {
        var current = cause
        while (current != null) {
            if (current is CredentialIssuanceError) return current
            current = current.cause
        }
        return null
    }

    private fun mapOAuthError(error: String?, description: String?): IssuanceError? {
        val normalized = description?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val normalizedError = error?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val mentionsTxCode = normalized.containsAny("tx_code", "tx code", "transaction code")
        val mentionsInvalid = normalized.containsAny("invalid", "wrong")
        val mentionsInactive = normalized.containsAny("inactive", "expired")
        val isInvalidRequest = normalizedError == "invalid_request"

        if (mentionsInactive) {
            return IssuanceError(
                resourceProvider.getString(R.string.issuance_document_offer_error_txcode_inactive),
                ErrorUtils.ISSUANCE_TX_CODE_INACTIVE
            )
        }

        if (mentionsTxCode && (mentionsInvalid || isInvalidRequest)) {
            return IssuanceError(
                resourceProvider.getString(R.string.issuance_document_offer_error_txcode_invalid),
                ErrorUtils.ISSUANCE_TX_CODE_INVALID
            )
        }

        return null
    }

    private fun String.containsAny(vararg tokens: String): Boolean =
        tokens.any { token -> contains(token, ignoreCase = true) }

    private fun extractIssuingAuthority(document: IssuedDocument): String? {
        return document.data.claims
            .firstOrNull { it.identifier == "issuing_authority" }
            ?.value
            ?.toString()
    }

    private suspend fun persistIssuanceMethod(
        documentId: DocumentId,
        issuanceMethod: IssuanceMethod,
    ) {
        documentIssuanceMethodDao.store(
            DocumentIssuanceMethod(
                documentId = documentId,
                issuanceMethod = issuanceMethod.value
            )
        )
    }
}
