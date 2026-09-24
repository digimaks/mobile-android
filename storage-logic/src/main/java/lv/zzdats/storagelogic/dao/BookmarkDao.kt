// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.storagelogic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import lv.zzdats.storagelogic.dao.type.StorageDao
import lv.zzdats.storagelogic.model.Bookmark

@Dao
interface BookmarkDao : StorageDao<Bookmark> {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    override suspend fun store(value: Bookmark)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    override suspend fun storeAll(values: List<Bookmark>)

    @Query("SELECT * FROM bookmarks WHERE identifier = :identifier")
    override suspend fun retrieve(identifier: String): Bookmark?

    @Query("SELECT * FROM bookmarks")
    override suspend fun retrieveAll(): List<Bookmark>

    @Update
    override suspend fun update(value: Bookmark)

    @Query("DELETE FROM bookmarks WHERE identifier = :identifier")
    override suspend fun delete(identifier: String)

    @Query("DELETE FROM bookmarks")
    override suspend fun deleteAll()
}