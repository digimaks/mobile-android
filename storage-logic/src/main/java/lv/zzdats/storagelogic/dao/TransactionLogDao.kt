// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.storagelogic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import lv.zzdats.storagelogic.dao.type.StorageDao
import lv.zzdats.storagelogic.model.TransactionLog

@Dao
interface TransactionLogDao : StorageDao<TransactionLog> {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    override suspend fun store(value: TransactionLog)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    override suspend fun storeAll(values: List<TransactionLog>)

    @Query("SELECT * FROM transactionLogs WHERE id = :identifier")
    override suspend fun retrieve(identifier: String): TransactionLog?

    @Query("SELECT * FROM transactionLogs")
    override suspend fun retrieveAll(): List<TransactionLog>

    @Query("SELECT * FROM transactionLogs WHERE documentId = :documentId")
    suspend fun retrieveByDocumentId(documentId: String): List<TransactionLog>

    @Update
    override suspend fun update(value: TransactionLog)

    @Query("DELETE FROM transactionLogs WHERE id = :identifier")
    override suspend fun delete(identifier: String)

    @Query("DELETE FROM transactionLogs")
    override suspend fun deleteAll()
}