// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.storagelogic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import lv.zzdats.storagelogic.dao.type.StorageDao
import lv.zzdats.storagelogic.model.EudiTransactionLog

@Dao
interface EudiTransactionLogDao : StorageDao<EudiTransactionLog> {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    override suspend fun store(value: EudiTransactionLog)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    override suspend fun storeAll(values: List<EudiTransactionLog>)

    @Query("SELECT * FROM eudiTransactionLogs WHERE identifier = :identifier")
    override suspend fun retrieve(identifier: String): EudiTransactionLog?

    @Query("SELECT * FROM eudiTransactionLogs")
    override suspend fun retrieveAll(): List<EudiTransactionLog>

    @Update
    override suspend fun update(value: EudiTransactionLog)

    @Query("DELETE FROM eudiTransactionLogs WHERE identifier = :identifier")
    override suspend fun delete(identifier: String)

    @Query("DELETE FROM eudiTransactionLogs")
    override suspend fun deleteAll()
}