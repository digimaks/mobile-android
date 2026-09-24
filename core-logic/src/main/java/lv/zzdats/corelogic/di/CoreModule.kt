// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.di

import android.content.Context
import eu.europa.ec.eudi.wallet.EudiWallet
import io.ktor.client.HttpClient
import lv.zzdats.businesslogic.config.EnvironmentConfig
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.businesslogic.controller.crypto.SecureAreaRepository
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.businesslogic.provider.UuidProvider
import lv.zzdats.corelogic.config.WalletConfig
import lv.zzdats.corelogic.config.WalletCoreConfigImpl
import lv.zzdats.corelogic.controller.AppUpdateStateController
import lv.zzdats.corelogic.controller.AppUpdateStateControllerImpl
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.controller.WalletCoreDocumentsControllerImpl
import lv.zzdats.corelogic.controller.WalletCoreLogController
import lv.zzdats.corelogic.controller.WalletCoreLogControllerImpl
import lv.zzdats.corelogic.controller.WalletCoreTransactionLogController
import lv.zzdats.corelogic.controller.WalletCoreTransactionLogControllerImpl
import lv.zzdats.corelogic.provider.WalletCoreAttestationProvider
import lv.zzdats.corelogic.provider.WalletCoreAttestationProviderImpl
import lv.zzdats.corelogic.security.SecureAreaController
import lv.zzdats.corelogic.security.SecureAreaControllerImpl
import lv.zzdats.corelogic.security.SecureAreaProvider
import lv.zzdats.corelogic.security.SecureAreaRepositoryImpl
import lv.zzdats.networklogic.repository.WalletAttestationRepository
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.storagelogic.dao.BookmarkDao
import lv.zzdats.storagelogic.dao.DocumentIssuanceMethodDao
import lv.zzdats.storagelogic.dao.EudiTransactionLogDao
import lv.zzdats.storagelogic.dao.RevokedDocumentDao
import lv.zzdats.storagelogic.dao.TransactionLogDao
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Single
import org.koin.core.annotation.Singleton
import org.koin.mp.KoinPlatform

const val PRESENTATION_SCOPE_ID = "presentation_scope_id"

@Module
@ComponentScan("lv.zzdats.corelogic")
class CoreModule

@Single
fun provideEudiWallet(
    context: Context,
    walletCoreConfig: WalletConfig,
    walletCoreLogController: WalletCoreLogController,
    walletCoreTransactionLogController: WalletCoreTransactionLogController,
    walletCoreAttestationProvider: WalletCoreAttestationProvider,
    httpClient: HttpClient
): EudiWallet = EudiWallet(
    context = context,
    config = walletCoreConfig.config,
    walletProvider = walletCoreAttestationProvider
) {
    withLogger(walletCoreLogController)
    withTransactionLogger(walletCoreTransactionLogController)
    withKtorHttpClientFactory { httpClient }
}
@Single
fun provideWalletCoreConfig(
    context: Context,
    environmentConfig: EnvironmentConfig,
): WalletConfig = WalletCoreConfigImpl(
    context,
    environmentConfig,
)
@Single
fun provideWalletCoreLogController(logController: LogController): WalletCoreLogController =
    WalletCoreLogControllerImpl(logController)

@Single
fun provideWalletCoreAttestationProvider(
    walletAttestationRepository: WalletAttestationRepository,
    walletCoreConfig: WalletConfig,
    secureAreaRepository: SecureAreaRepository,
    prefKeys: PrefKeys
): WalletCoreAttestationProvider =
    WalletCoreAttestationProviderImpl(
        walletCoreConfig = walletCoreConfig,
        walletAttestationRepository = walletAttestationRepository,
        secureAreaRepository = secureAreaRepository,
        prefKeys = prefKeys
    )

@Single
fun provideAppUpdateStateController(): AppUpdateStateController = AppUpdateStateControllerImpl()

@Singleton
fun provideSecureAreaProvider(): SecureAreaProvider {
    return SecureAreaProvider.getInstance()
}

@Single
fun provideSecureAreaManager(
    context: Context,
    secureAreaProvider: SecureAreaProvider,
    prefKeys: PrefKeys,
    eudiWallet: Lazy<EudiWallet>,
    environmentConfig: EnvironmentConfig,
    logController: LogController
): SecureAreaController = SecureAreaControllerImpl(
    context,
    prefKeys,
    secureAreaProvider,
    eudiWallet,
    environmentConfig,
    logController
)

@Singleton
fun provideSecureAreaRepository(
    secureAreaController: SecureAreaController
): SecureAreaRepository = SecureAreaRepositoryImpl(secureAreaController)

@Single
fun provideWalletCoreDocumentsController(
    resourceProvider: ResourceProvider,
    eudiWallet: EudiWallet,
    walletConfig: WalletConfig,
    bookmarkDao: BookmarkDao,
    transactionLogDao: TransactionLogDao,
    documentIssuanceMethodDao: DocumentIssuanceMethodDao,
    revokedDocumentDao: RevokedDocumentDao
): WalletCoreDocumentsController =
    WalletCoreDocumentsControllerImpl(
        resourceProvider,
        eudiWallet,
        walletConfig,
        transactionLogDao,
        bookmarkDao,
        documentIssuanceMethodDao,
        revokedDocumentDao
    )

@Single
fun provideWalletCoreTransactionLogController(
    eudiTransactionLogDao: EudiTransactionLogDao,
    uuidProvider: UuidProvider
): WalletCoreTransactionLogController = WalletCoreTransactionLogControllerImpl(
    eudiTransactionLogDao = eudiTransactionLogDao,
    uuidProvider = uuidProvider
)

@Scope
class WalletPresentationScope

fun getOrCreatePresentationScope(): org.koin.core.scope.Scope =
    KoinPlatform.getKoin().getOrCreateScope<WalletPresentationScope>(PRESENTATION_SCOPE_ID)

