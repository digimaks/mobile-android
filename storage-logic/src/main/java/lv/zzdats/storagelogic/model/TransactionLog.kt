// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.storagelogic.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType {
    DOCUMENT_ISSUED,
    DOCUMENT_DELETED,
    DOCUMENT_PRESENTED,
    DOCUMENT_SIGNED
}

@Entity(tableName = "transactionLogs")
data class TransactionLog(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val docType: String,
    val nameSpace: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String,
    val authority: String? = null,
    val eventType: String
)