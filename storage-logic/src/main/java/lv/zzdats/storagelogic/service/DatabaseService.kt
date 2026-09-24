// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.storagelogic.service

import androidx.room.Database
import androidx.room.RoomDatabase
import lv.zzdats.storagelogic.dao.BookmarkDao
import lv.zzdats.storagelogic.dao.DocumentIssuanceMethodDao
import lv.zzdats.storagelogic.dao.EudiTransactionLogDao
import lv.zzdats.storagelogic.dao.RevokedDocumentDao
import lv.zzdats.storagelogic.dao.TransactionLogDao
import lv.zzdats.storagelogic.model.Bookmark
import lv.zzdats.storagelogic.model.DocumentIssuanceMethod
import lv.zzdats.storagelogic.model.EudiTransactionLog
import lv.zzdats.storagelogic.model.RevokedDocument
import lv.zzdats.storagelogic.model.TransactionLog

@Database(
    entities = [
        Bookmark::class,
        DocumentIssuanceMethod::class,
        TransactionLog::class,
        EudiTransactionLog::class,
        RevokedDocument::class
    ],
    version = 2
)
abstract class DatabaseService : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun documentIssuanceMethodDao(): DocumentIssuanceMethodDao
    abstract fun transactionLogDao(): TransactionLogDao
    abstract fun eudiTransactionLogDao(): EudiTransactionLogDao
    abstract fun revokedDocumentDao(): RevokedDocumentDao
}
