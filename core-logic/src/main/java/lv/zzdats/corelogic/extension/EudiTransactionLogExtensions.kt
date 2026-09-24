// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.extension

import com.nimbusds.jose.shaded.gson.Gson
import eu.europa.ec.eudi.wallet.transactionLogging.TransactionLog
import eu.europa.ec.eudi.wallet.transactionLogging.presentation.PresentationTransactionLog
import lv.zzdats.businesslogic.util.toLocalDate
import lv.zzdats.businesslogic.util.toLocalDateTime
import lv.zzdats.corelogic.model.TransactionLogDataDomain
import lv.zzdats.corelogic.model.TransactionLogDataDomain.PresentationLog
import lv.zzdats.storagelogic.model.EudiTransactionLog as StorageTransaction


internal fun StorageTransaction.toCoreTransactionLog(): TransactionLog? = try {
    Gson().fromJson(
        this.value,
        TransactionLog::class.java
    )
} catch (_: Exception) {
    null
}

// TODO RETURN PROPER OBJECTS ONCE READY FROM CORE ISSUANCE,SIGNING
@Throws(IllegalArgumentException::class)
internal fun TransactionLog.parseTransactionLog(): Any? =
    when (this.type) {
        TransactionLog.Type.Presentation ->
            PresentationTransactionLog.fromTransactionLog(this)
                .getOrNull()

        TransactionLog.Type.Issuance ->
            throw IllegalArgumentException("UnSupported transaction log type")

        TransactionLog.Type.Signing ->
            throw IllegalArgumentException("UnSupported transaction log type")
    }

// TODO RETURN PROPER OBJECTS ONCE READY FROM CORE ISSUANCE,SIGNING
@Throws(IllegalArgumentException::class)
internal fun Any.toTransactionLogData(id: String): TransactionLogDataDomain = when (this) {
    is PresentationTransactionLog -> PresentationLog(
        id = id,
        name = relyingParty.name,
        status = status,
        creationLocalDateTime = timestamp.toLocalDateTime(),
        creationLocalDate = timestamp.toLocalDate(),
        relyingParty = relyingParty,
        documents = documents,
    )

    else -> throw IllegalArgumentException("Unknown transaction log type")
}