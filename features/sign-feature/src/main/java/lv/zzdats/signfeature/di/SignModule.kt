// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.signfeature.di

import android.content.Context
import lv.zzdats.authlogic.service.AuthService
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.networklogic.api.wallet.WalletApiClient
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.signfeature.interactor.EParakstIdentitiesInteractor
import lv.zzdats.signfeature.interactor.EParakstIdentitiesInteractorImpl
import lv.zzdats.signfeature.interactor.SignDocumentInteractor
import lv.zzdats.signfeature.interactor.SignDocumentInteractorImpl
import lv.zzdats.signfeature.util.FilePickerHelper
import lv.zzdats.storagelogic.dao.TransactionLogDao
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.zzdats.signfeature")
class SignModule

@Single
fun provideFilePickerHelper(context: Context): FilePickerHelper = FilePickerHelper(context)

@Factory
fun provideSignDocumentInteractor(
    walletApiClient: WalletApiClient,
    resourceProvider: ResourceProvider,
    authService: AuthService,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    transactionLogDao: TransactionLogDao
): SignDocumentInteractor = SignDocumentInteractorImpl(
    walletApiClient,
    resourceProvider,
    authService,
    walletCoreDocumentsController,
    transactionLogDao
)

@Factory
fun provideEParakstIdentitiesInteractor(
    walletApiClient: WalletApiClient,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider
): EParakstIdentitiesInteractor = EParakstIdentitiesInteractorImpl(
    walletApiClient,
    walletCoreDocumentsController,
    resourceProvider
)