// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.worker

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy
import kotlinx.coroutines.flow.first
import lv.zzdats.corelogic.config.WalletConfig
import lv.zzdats.corelogic.controller.IssueDocumentsPartialState
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.util.CoreActions
import lv.zzdats.corelogic.util.CoreActions.RE_ISSUANCE_IDS_DETAILS_EXTRA
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.Instant
import kotlin.getValue

class ReIssuanceWorkManager(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val walletCoreDocumentsController: WalletCoreDocumentsController by inject()
    private val walletConfig: WalletConfig by inject()

    companion object {
        const val RE_ISSUANCE_WORK_NAME = "reIssuanceWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val rule = walletConfig.documentIssuanceConfig.reissuanceRule

            val expirationLimit = Instant.now().plus(Duration.ofHours(rule.minExpirationHours.toLong()))
            val reIssuedDocumentIds = mutableListOf<String>()

            walletCoreDocumentsController
                .getAllIssuedDocuments()
                .filter { document ->
                    val belowMinCount =
                        document.credentialsCount() <= rule.minNumberOfCredentials

                    val isOneTimeUse =
                        document.credentialPolicy == CredentialPolicy.OneTimeUse

                    val expiresWithinThreshold =
                        document.getValidUntil()
                            .map { validUntil -> validUntil.isBefore(expirationLimit) }
                            .getOrDefault(false)

                    (belowMinCount && isOneTimeUse) || expiresWithinThreshold
                }
                .forEach { document ->
                    val state = walletCoreDocumentsController.reIssueDocument(
                        documentId = document.id,
                        issuerId = document.issuerMetadata?.credentialIssuerIdentifier?.toString().orEmpty(),
                        allowAuthorizationFallback = false
                    ).first()

                    when (state) {
                        is IssueDocumentsPartialState.DeferredSuccess,
                        is IssueDocumentsPartialState.PartialSuccess,
                        is IssueDocumentsPartialState.Success -> {
                            reIssuedDocumentIds.add(document.id)
                        }

                        is IssueDocumentsPartialState.Failure -> Unit
                        is IssueDocumentsPartialState.UserAuthRequired -> state.resultHandler.onAuthenticationFailure
                    }
                }

            if (reIssuedDocumentIds.isNotEmpty()) {
                notifyDocumentsList()
                notifyDocumentDetails(reIssuedDocumentIds)
            }

            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private fun notifyDocumentDetails(reIssuedIds: List<String>) {
        val detailsIntent = Intent(CoreActions.RE_ISSUANCE_WORK_REFRESH_DETAILS_ACTION).apply {
            putStringArrayListExtra(
                RE_ISSUANCE_IDS_DETAILS_EXTRA,
                ArrayList(reIssuedIds)
            )
        }
        applicationContext.sendBroadcast(detailsIntent)
    }

    private fun notifyDocumentsList() {
        val refreshIntent = Intent(CoreActions.RE_ISSUANCE_WORK_REFRESH_ACTION)
        applicationContext.sendBroadcast(refreshIntent)
    }
}
