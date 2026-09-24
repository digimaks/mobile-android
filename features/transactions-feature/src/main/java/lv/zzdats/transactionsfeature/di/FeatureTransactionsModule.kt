// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.transactionsfeature.di

import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.storagelogic.dao.TransactionLogDao
import lv.zzdats.transactionsfeature.ui.TransactionsInteractor
import lv.zzdats.transactionsfeature.ui.TransactionsInteractorImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("lv.zzdats.transactionsfeature")
class FeatureTransactionsModule

@Factory
fun provideTransactionsInteractor(
    resourceProvider: ResourceProvider,
    transactionLogDao: TransactionLogDao
): TransactionsInteractor =
    TransactionsInteractorImpl(
        resourceProvider,
        transactionLogDao
    )