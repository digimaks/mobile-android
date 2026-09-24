// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.controller

import com.nimbusds.jose.shaded.gson.Gson
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lv.zzdats.businesslogic.provider.UuidProvider
import lv.zzdats.storagelogic.dao.EudiTransactionLogDao
import kotlin.uuid.ExperimentalUuidApi
import lv.zzdats.storagelogic.model.EudiTransactionLog as TransactionStorage

interface WalletCoreTransactionLogController : TransactionLogger

class WalletCoreTransactionLogControllerImpl(
    private val eudiTransactionLogDao: EudiTransactionLogDao,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val uuidProvider: UuidProvider
) : WalletCoreTransactionLogController {

    @OptIn(ExperimentalUuidApi::class)
    override fun log(transaction: TransactionLog) {
        scope.launch {
            val json = Gson().toJson(transaction)
            eudiTransactionLogDao.store(
                TransactionStorage(
                    identifier = uuidProvider.provideUuid(),
                    value = json
                )
            )
        }
    }
}