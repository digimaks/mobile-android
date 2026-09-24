// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.dashboardfeature.interactor

import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.Uri
import eu.europa.ec.eudi.wallet.document.Document
import eu.europa.ec.eudi.wallet.document.DocumentId
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import lv.zzdats.businesslogic.config.ConfigLogic
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.businesslogic.extensions.compareLocaleLanguage
import lv.zzdats.businesslogic.extensions.safeAsync
import lv.zzdats.commonfeature.features.document_details.model.DocumentUi
import lv.zzdats.commonfeature.features.document_details.model.DocumentUiIssuanceState
import lv.zzdats.commonfeature.features.document_details.transformer.DocumentDetailsTransformer
import lv.zzdats.commonfeature.features.document_details.transformer.transformToDocumentDetail
import lv.zzdats.commonfeature.util.DocumentFieldExtractor
import lv.zzdats.commonfeature.util.DocumentFieldExtractor.extractFirstName
import lv.zzdats.commonfeature.util.DocumentFieldExtractor.extractLastName
import lv.zzdats.commonfeature.util.DocumentFieldExtractor.removePnolvPrefix
import lv.zzdats.commonfeature.util.DocumentJsonKeys
import lv.zzdats.commonfeature.util.convertAnyToFormattedDate
import lv.zzdats.commonfeature.util.documentHasExpired
import lv.zzdats.commonfeature.util.extractValueFromDocumentOrEmpty
import lv.zzdats.corelogic.config.WalletConfig
import lv.zzdats.corelogic.controller.DeleteDocumentPartialState
import lv.zzdats.corelogic.controller.IssueDeferredDocumentPartialState
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.extension.localizedDocumentMetadata
import lv.zzdats.corelogic.extension.localizedIssuerMetadata
import lv.zzdats.corelogic.model.DeferredDocumentData
import lv.zzdats.corelogic.model.DocumentIdentifier
import lv.zzdats.corelogic.model.FormatType
import lv.zzdats.corelogic.model.toDocumentIdentifier
import lv.zzdats.dashboardfeature.model.UserInfo
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.storagelogic.dao.BookmarkDao

sealed class DashboardInteractorGetDocumentsPartialState {
    data class Success(
        val documentsUi: List<DocumentUi>,
        val mainPid: IssuedDocument?,
        val userFirstName: String,
        val userBase64Portrait: String,
    ) : DashboardInteractorGetDocumentsPartialState()

    data class Failure(val error: String) : DashboardInteractorGetDocumentsPartialState()
}

sealed class DashboardInteractorDeleteDocumentPartialState {
    data object SingleDocumentDeleted : DashboardInteractorDeleteDocumentPartialState()
    data object AllDocumentsDeleted : DashboardInteractorDeleteDocumentPartialState()
    data class Failure(val errorMessage: String) :
        DashboardInteractorDeleteDocumentPartialState()
}

sealed class DashboardInteractorRetryIssuingDeferredDocumentPartialState {
    data class Success(
        val deferredDocumentData: DeferredDocumentData
    ) : DashboardInteractorRetryIssuingDeferredDocumentPartialState()

    data class NotReady(
        val deferredDocumentData: DeferredDocumentData
    ) : DashboardInteractorRetryIssuingDeferredDocumentPartialState()

    data class Failure(
        val documentId: DocumentId,
        val errorMessage: String,
    ) : DashboardInteractorRetryIssuingDeferredDocumentPartialState()

    data class Expired(
        val documentId: DocumentId,
    ) : DashboardInteractorRetryIssuingDeferredDocumentPartialState()
}

sealed class DashboardInteractorRetryIssuingDeferredDocumentsPartialState {
    data class Result(
        val successfullyIssuedDeferredDocuments: List<DeferredDocumentData>,
        val failedIssuedDeferredDocuments: List<DocumentId>,
    ) : DashboardInteractorRetryIssuingDeferredDocumentsPartialState()

    data class Failure(
        val errorMessage: String,
    ) : DashboardInteractorRetryIssuingDeferredDocumentsPartialState()
}

interface DashboardInteractor {
    fun getDocuments(): Flow<DashboardInteractorGetDocumentsPartialState>
    fun isBleAvailable(): Boolean
    fun isBleCentralClientModeEnabled(): Boolean
    fun getAppVersion(): String
    fun deleteDocument(
        documentId: String
    ): Flow<DashboardInteractorDeleteDocumentPartialState>

    fun tryIssuingDeferredDocumentsFlow(
        deferredDocuments: Map<DocumentId, FormatType>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Flow<DashboardInteractorRetryIssuingDeferredDocumentsPartialState>

    fun retrieveLogFileUris(): ArrayList<Uri>
}

class DashboardInteractorImpl(
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val walletCoreConfig: WalletConfig,
    private val configLogic: ConfigLogic,
    private val logController: LogController,
    private val bookmarkDao: BookmarkDao
) : DashboardInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun isBleAvailable(): Boolean {
        val bluetoothManager: BluetoothManager? = resourceProvider.provideContext()
            .getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter?.isEnabled == true
    }

    override fun isBleCentralClientModeEnabled(): Boolean =
        walletCoreConfig.config.enableBleCentralMode

    override fun getDocuments(): Flow<DashboardInteractorGetDocumentsPartialState> = flow {
        var userFirstName = ""
        var userImage = ""
        val documents = walletCoreDocumentsController.getAllDocuments()

        val mainPid = walletCoreDocumentsController.getMainPidDocument()
        val documentsUi = documents.map { document ->

            val documentIsBookmarked = bookmarkDao.retrieve(document.id) != null

            val (documentUi, userInfo) = document.toDocumentUiAndUserInfo(mainPid, documentIsBookmarked)

            if (userFirstName.isBlank()) {
                userFirstName = userInfo.userFirstName
            }
            if (userImage.isBlank()) {
                userImage = userInfo.userBase64Portrait
            }

            return@map documentUi
        }
        emit(
            DashboardInteractorGetDocumentsPartialState.Success(
                documentsUi = documentsUi,
                mainPid = mainPid,
                userFirstName = userFirstName,
                userBase64Portrait = userImage
            )
        )
    }.safeAsync {
        DashboardInteractorGetDocumentsPartialState.Failure(
            error = it.localizedMessage ?: genericErrorMsg
        )
    }

    override fun getAppVersion(): String = configLogic.appVersion

    override fun deleteDocument(
        documentId: String,
    ): Flow<DashboardInteractorDeleteDocumentPartialState> =
        flow {
            walletCoreDocumentsController.deleteDocument(documentId).collect { response ->
                when (response) {
                    is DeleteDocumentPartialState.Failure -> {
                        emit(
                            DashboardInteractorDeleteDocumentPartialState.Failure(
                                errorMessage = response.errorMessage
                            )
                        )
                    }

                    is DeleteDocumentPartialState.Success -> {
                        if (walletCoreDocumentsController.getAllDocuments().isEmpty()) {
                            emit(DashboardInteractorDeleteDocumentPartialState.AllDocumentsDeleted)
                        } else
                            emit(DashboardInteractorDeleteDocumentPartialState.SingleDocumentDeleted)
                    }
                }
            }
        }.safeAsync {
            DashboardInteractorDeleteDocumentPartialState.Failure(
                errorMessage = it.localizedMessage ?: genericErrorMsg
            )
        }


    override fun tryIssuingDeferredDocumentsFlow(
        deferredDocuments: Map<DocumentId, FormatType>,
        dispatcher: CoroutineDispatcher,
    ): Flow<DashboardInteractorRetryIssuingDeferredDocumentsPartialState> = flow {

        val successResults: MutableList<DeferredDocumentData> = mutableListOf()
        val failedResults: MutableList<DocumentId> = mutableListOf()

        withContext(dispatcher) {
            val allJobs = deferredDocuments.keys.map { deferredDocumentId ->
                async {
                    tryIssuingDeferredDocumentSuspend(deferredDocumentId)
                }
            }

            allJobs.forEach { job ->
                when (val result = job.await()) {
                    is DashboardInteractorRetryIssuingDeferredDocumentPartialState.Failure -> {
                        failedResults.add(result.documentId)
                    }

                    is DashboardInteractorRetryIssuingDeferredDocumentPartialState.Success -> {
                        successResults.add(result.deferredDocumentData)
                    }

                    is DashboardInteractorRetryIssuingDeferredDocumentPartialState.NotReady -> {}

                    is DashboardInteractorRetryIssuingDeferredDocumentPartialState.Expired -> {
                        deleteDocument(result.documentId)
                    }
                }
            }
        }

        emit(
            DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Result(
                successfullyIssuedDeferredDocuments = successResults,
                failedIssuedDeferredDocuments = failedResults
            )
        )

    }.safeAsync {
        DashboardInteractorRetryIssuingDeferredDocumentsPartialState.Failure(
            errorMessage = it.localizedMessage ?: genericErrorMsg
        )
    }

    override fun retrieveLogFileUris(): ArrayList<Uri> {
        return ArrayList(logController.retrieveLogFileUris())
    }

    private suspend fun Document.toDocumentUiAndUserInfo(mainPid: IssuedDocument?, documentIsBookmarked: Boolean): Pair<DocumentUi, UserInfo> {
        val documentIsRevoked =
            walletCoreDocumentsController.isDocumentRevoked(this.id)

        when (this) {
            is IssuedDocument -> {
                val userLocale = resourceProvider.getLocale()

                val documentExpirationDate: String = getDocumentExpiryDate(this)

                val documentHasExpired = if (documentExpirationDate != null) {
                    documentHasExpired(documentExpirationDate = documentExpirationDate)
                } else {
                    false
                }

                val userFirstName = extractValueFromDocumentOrEmpty(
                    document = mainPid ?: this,
                    key = DocumentJsonKeys.FIRST_NAME
                )

                val userImage = extractValueFromDocumentOrEmpty(
                    document = this,
                    key = DocumentJsonKeys.PORTRAIT
                )

                val issuanceDate = getDocumentIssuanceDate(this)

                val documentIdentifier = this.toDocumentIdentifier()

                val displayNumber = DocumentFieldExtractor.extractDisplayNumber(documentIdentifier, this).removePnolvPrefix()
                val description = DocumentFieldExtractor.extractDescription(documentIdentifier, this)
                val additionalInfo = DocumentFieldExtractor.extractAdditionalInfo(documentIdentifier, this)
                val description1 = extractFirstName(documentIdentifier, this)
                val description2 = extractLastName(documentIdentifier, this)

                val issuingAuthority =  extractValueFromDocumentOrEmpty(this, DocumentJsonKeys.ISSUING_AUTHORITY)
                val issuanceMethod = walletCoreDocumentsController.getDocumentIssuanceMethod(this.id)?.value

                val documentIssuanceState = when {
                    documentIsRevoked -> DocumentUiIssuanceState.Revoked
                    documentHasExpired -> DocumentUiIssuanceState.Expired
                    else -> DocumentUiIssuanceState.Issued
                }


                val issuerDisplay = this.localizedIssuerMetadata(userLocale)
                val documentDisplay = this.localizedDocumentMetadata(userLocale)
                val detailsItems = DocumentDetailsTransformer.transformToUiItem(
                    document = this,
                    resourceProvider = resourceProvider,
                    walletCoreDocumentsController = walletCoreDocumentsController
                )?.documentDetails ?: this.data.claims.map { claim ->
                    transformToDocumentDetail(
                        displayKey = claim.issuerMetadata?.display?.firstOrNull {
                            resourceProvider.getLocale().compareLocaleLanguage(it.locale)
                        }?.name ?: claim.issuerMetadata?.display?.firstOrNull()?.name,
                        key = claim.identifier,
                        item = claim.value ?: "",
                        resourceProvider = resourceProvider
                    )
                }

                return DocumentUi(
                    documentId = this.id,
                    documentName = this.name,
                    documentIdentifier = this.toDocumentIdentifier(),
                    documentHasExpired = documentHasExpired,
                    documentDetails = detailsItems,
                    documentIssuanceState = documentIssuanceState,
                    documentExpirationDate = documentExpirationDate,
                    issuingAuthority = issuingAuthority,
                    issuerCountry = getDocumentIssuerCountry(this),
                    issuanceDate = issuanceDate,
                    issuanceMethod = issuanceMethod,
                    documentIsBookmarked = documentIsBookmarked,
                    displayNumber = displayNumber,
                    description = description,
                    additionalInfo = additionalInfo,
                    issuerDisplay = issuerDisplay,
                    documentDisplay = documentDisplay,
                    description1 = "$description1 $description2",
                    description2 = displayNumber,
                ) to UserInfo(
                    userFirstName = userFirstName,
                    userBase64Portrait = userImage
                )
            }

            else -> {
                return DocumentUi(
                    documentId = this.id,
                    documentName = this.name,
                    documentIdentifier = this.toDocumentIdentifier(),
                    documentHasExpired = false,
                    documentDetails = emptyList(),
                    documentIssuanceState = DocumentUiIssuanceState.Pending,
                    documentExpirationDate = "",
                    issuingAuthority = "",
                    issuanceMethod = walletCoreDocumentsController.getDocumentIssuanceMethod(this.id)?.value,
                    documentIsBookmarked = false
                ) to UserInfo(
                    userFirstName = "",
                    userBase64Portrait = ""
                )
            }
        }
    }

    private fun getDocumentExpiryDate(document: IssuedDocument): String {
        val possibleKeys = listOf(
            DocumentJsonKeys.EXPIRY_DATE,    // PID
            DocumentJsonKeys.SIGNING_EXPIRY_DATE,   // SIGNING
            DocumentJsonKeys.JWT_EXPIRTY_DATE
        )

        return possibleKeys.firstNotNullOfOrNull { key ->
            val value = extractValueFromDocumentOrEmpty(
                document = document,
                key = key
            )
            val formatted = convertAnyToFormattedDate(value)
            if (!formatted.isNullOrEmpty()) formatted else null
        } ?: ""
    }

    private fun getDocumentIssuanceDate(document: IssuedDocument): String {
        val possibleKeys = listOf(
            DocumentJsonKeys.ISSUANCE_DATE,    // PID
            DocumentJsonKeys.ISSUE_DATE,   // MDL
            DocumentJsonKeys.DIPLOMA_ISSUANCE_DATE,  // Diploma
            DocumentJsonKeys.SIGNING_ISSUANCE_DATE,
            DocumentJsonKeys.JWT_ISSUED_DATE
        )

        val claimDate = possibleKeys.firstNotNullOfOrNull { key ->
            val value = extractValueFromDocumentOrEmpty(
                document = document,
                key = key
            )
            val formatted = convertAnyToFormattedDate(value)
            if (!formatted.isNullOrEmpty()) formatted else null
        }

        if (!claimDate.isNullOrEmpty()) return claimDate

        return convertAnyToFormattedDate(document.createdAt) ?: ""
    }

    private fun getDocumentIssuerCountry(document: IssuedDocument): String {
        val possibleKeys = listOf(
            DocumentJsonKeys.ISSUER_COUNTRY,      // Standard
            DocumentJsonKeys.DIPLOMA_ISSUER_COUNTRY // Diploma
        )

        return possibleKeys.firstNotNullOfOrNull { key ->
            val value = extractValueFromDocumentOrEmpty(
                document = document,
                key = key
            )
            if (value.isNotEmpty()) value else null
        } ?: ""
    }

    private suspend fun tryIssuingDeferredDocumentSuspend(
        deferredDocumentId: DocumentId,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): DashboardInteractorRetryIssuingDeferredDocumentPartialState {
        return withContext(dispatcher) {
            walletCoreDocumentsController.issueDeferredDocument(deferredDocumentId)
                .map { result ->
                    when (result) {
                        is IssueDeferredDocumentPartialState.Failed -> {
                            DashboardInteractorRetryIssuingDeferredDocumentPartialState.Failure(
                                documentId = result.documentId,
                                errorMessage = result.errorMessage
                            )
                        }

                        is IssueDeferredDocumentPartialState.Issued -> {
                            DashboardInteractorRetryIssuingDeferredDocumentPartialState.Success(
                                deferredDocumentData = result.deferredDocumentData
                            )
                        }

                        is IssueDeferredDocumentPartialState.NotReady -> {
                            DashboardInteractorRetryIssuingDeferredDocumentPartialState.NotReady(
                                deferredDocumentData = result.deferredDocumentData
                            )
                        }

                        is IssueDeferredDocumentPartialState.Expired -> {
                            DashboardInteractorRetryIssuingDeferredDocumentPartialState.Expired(
                                documentId = result.documentId
                            )
                        }
                    }
                }.firstOrNull()
                ?: DashboardInteractorRetryIssuingDeferredDocumentPartialState.Failure(
                    documentId = deferredDocumentId,
                    errorMessage = genericErrorMsg
                )
        }
    }
}
