// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.storagelogic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import lv.zzdats.storagelogic.dao.type.StorageDao
import lv.zzdats.storagelogic.model.RevokedDocument

@Dao
interface RevokedDocumentDao : StorageDao<RevokedDocument> {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    override suspend fun store(value: RevokedDocument)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    override suspend fun storeAll(values: List<RevokedDocument>)

    @Query("SELECT * FROM revokedDocuments WHERE identifier = :identifier")
    override suspend fun retrieve(identifier: String): RevokedDocument?

    @Query("SELECT * FROM revokedDocuments")
    override suspend fun retrieveAll(): List<RevokedDocument>

    @Update
    override suspend fun update(value: RevokedDocument)

    @Query("DELETE FROM revokedDocuments WHERE identifier = :identifier")
    override suspend fun delete(identifier: String)

    @Query("DELETE FROM revokedDocuments")
    override suspend fun deleteAll()
}