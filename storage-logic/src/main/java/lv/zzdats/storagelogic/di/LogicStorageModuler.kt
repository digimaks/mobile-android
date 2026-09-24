// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.storagelogic.di

import android.content.Context
import androidx.room.Room
import lv.zzdats.storagelogic.dao.BookmarkDao
import lv.zzdats.storagelogic.dao.DocumentIssuanceMethodDao
import lv.zzdats.storagelogic.dao.EudiTransactionLogDao
import lv.zzdats.storagelogic.dao.RevokedDocumentDao
import lv.zzdats.storagelogic.dao.TransactionLogDao
import lv.zzdats.storagelogic.service.DatabaseService
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.zzdats.storagelogic")
class LogicStorageModule

@Single
fun provideAppDatabase(context: Context): DatabaseService =
    Room.databaseBuilder(
        context,
        DatabaseService::class.java,
        "eudi.app.wallet.storage"
    ).fallbackToDestructiveMigration(true).build()

@Single
fun provideBookmarkDao(service: DatabaseService): BookmarkDao = service.bookmarkDao()

@Single
fun provideDocumentIssuanceMethodDao(service: DatabaseService): DocumentIssuanceMethodDao =
    service.documentIssuanceMethodDao()

@Single
fun provideRevokedDocumentDao(service: DatabaseService): RevokedDocumentDao =
    service.revokedDocumentDao()

@Single
fun provideTransactionLogDao(service: DatabaseService): TransactionLogDao =
    service.transactionLogDao()

@Single
fun provideEudiTransactionLogDao(service: DatabaseService): EudiTransactionLogDao =
    service.eudiTransactionLogDao()
