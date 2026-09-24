// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.worker

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.europa.ec.eudi.statium.Status
import eu.europa.ec.eudi.wallet.document.IssuedDocument
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.model.RevokedDocumentDataDomain
import lv.zzdats.corelogic.util.CoreActions
import lv.zzdats.corelogic.util.CoreActions.REVOCATION_IDS_DETAILS_EXTRA
import lv.zzdats.storagelogic.dao.RevokedDocumentDao
import lv.zzdats.storagelogic.model.RevokedDocument
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

/**
 * [RevocationWorkManager] is a [CoroutineWorker] responsible for checking the revocation status of issued documents
 * and updating the local storage and sending broadcasts when revocations are detected.
 *
 * It utilizes Koin for dependency injection to obtain instances of [eu.europa.ec.storagelogic.dao.RevokedDocumentDao] and [WalletCoreDocumentsController].
 *
 * Key functionalities:
 *  - Periodically retrieves all issued documents from the [WalletCoreDocumentsController].
 *  - Checks the status of each document for revocation using [WalletCoreDocumentsController.resolveDocumentStatus].
 *  - Identifies documents with statuses [Status.Invalid] or [Status.Suspended] as revoked.
 *  - Stores revoked documents in the [eu.europa.ec.storagelogic.dao.RevokedDocumentDao].
 *  - Sends three broadcasts to notify the application about the revoked documents:
 *      - `CoreActions.REVOCATION_WORK_MESSAGE_ACTION`: Includes a list of [RevokedDocumentPayload] with names and IDs.
 *      - `CoreActions.REVOCATION_WORK_REFRESH_ACTION`: A general refresh action without specific data.
 *      - `CoreActions.REVOCATION_WORK_REFRESH_DETAILS_ACTION`: Includes a list of revoked document IDs in `REVOCATION_IDS_DETAILS_EXTRA`.
 *
 *  The worker returns:
 *      - [Result.success] if the revocation check and updates were successful.
 *      - [Result.failure] if an [IllegalArgumentException] occurred during the process.  This indicates a configuration or data issue.
 *
 * @param appContext The application context.
 * @param workerParams Parameters for the worker.
 */
class RevocationWorkManager(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val revokedDocumentDao: RevokedDocumentDao by inject()
    private val walletCoreDocumentsController: WalletCoreDocumentsController by inject()

    companion object {
        const val REVOCATION_WORK_NAME = "revocationWorker"
    }

    override suspend fun doWork(): Result {
        try {

            val storedRevokedDocuments = walletCoreDocumentsController.getRevokedDocumentIds()
            val fromRevokedToValid = mutableListOf<String>()
            val revokedDocuments = mutableListOf<IssuedDocument>()

            walletCoreDocumentsController
                .getAllIssuedDocuments()
                .forEach { document ->
                    walletCoreDocumentsController.resolveDocumentStatus(document).fold(
                        onSuccess = { status ->
                            when (status) {
                                is Status.Invalid, Status.Suspended -> {
                                    if (!storedRevokedDocuments.any { it == document.id }) {
                                        revokedDocuments.add(document)
                                    }
                                }

                                is Status.Valid -> {
                                    if (storedRevokedDocuments.any { it == document.id }) {
                                        fromRevokedToValid.add(document.id)
                                    }
                                }

                                else -> {}
                            }
                        },
                        onFailure = {}
                    )
                }

            if (fromRevokedToValid.isNotEmpty()) {
                removeRevokedDocumentsFromStorage(fromRevokedToValid)
            }

            if (revokedDocuments.isNotEmpty()) {
                storeRevokedDocuments(revokedDocuments)
                sendRevocationBroadcasts(revokedDocuments)
            }

            if (fromRevokedToValid.isNotEmpty() || revokedDocuments.isNotEmpty()) {
                notifyDocumentsList()
            }

            return Result.success()
        } catch (_: IllegalArgumentException) {
            return Result.failure()
        }
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun storeRevokedDocuments(revokedDocuments: List<IssuedDocument>) {
        revokedDocumentDao.storeAll(
            revokedDocuments.map { RevokedDocument(identifier = it.id) }
        )
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun removeRevokedDocumentsFromStorage(ids: List<String>) {
        ids.forEach {
            revokedDocumentDao.delete(it)
        }
    }

    private fun sendRevocationBroadcasts(revokedDocuments: List<IssuedDocument>) {

        val messageIntent = Intent(CoreActions.REVOCATION_WORK_MESSAGE_ACTION).apply {
            putParcelableArrayListExtra(
                CoreActions.REVOCATION_IDS_EXTRA,
                ArrayList(
                    revokedDocuments.map {
                        RevokedDocumentDataDomain(name = it.name, id = it.id)
                    }
                )
            )
        }

        val detailsIntent = Intent(CoreActions.REVOCATION_WORK_REFRESH_DETAILS_ACTION).apply {
            putStringArrayListExtra(
                REVOCATION_IDS_DETAILS_EXTRA,
                ArrayList(
                    revokedDocuments.map { it.id }
                )
            )
        }

        applicationContext.sendBroadcast(messageIntent)
        applicationContext.sendBroadcast(detailsIntent)
    }

    private fun notifyDocumentsList() {
        val refreshIntent = Intent(CoreActions.REVOCATION_WORK_REFRESH_ACTION)
        applicationContext.sendBroadcast(refreshIntent)
    }
}