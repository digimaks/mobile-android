// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.storagelogic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import lv.zzdats.storagelogic.model.DocumentIssuanceMethod

@Dao
interface DocumentIssuanceMethodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun store(documentIssuanceMethod: DocumentIssuanceMethod)

    @Query("SELECT * FROM documentIssuanceMethods WHERE documentId = :documentId")
    suspend fun retrieve(documentId: String): DocumentIssuanceMethod?

    @Query("DELETE FROM documentIssuanceMethods WHERE documentId = :documentId")
    suspend fun delete(documentId: String)
}
