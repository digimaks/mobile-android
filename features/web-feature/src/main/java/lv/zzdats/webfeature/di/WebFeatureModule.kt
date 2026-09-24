// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.webfeature.di

import lv.zzdats.authlogic.service.AuthService
import lv.zzdats.businesslogic.config.ConfigLogic
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.zzdats.commonfeature.features.biometric.BiometricInteractor
import lv.zzdats.commonfeature.features.settings.SettingsBridge
import lv.zzdats.commonfeature.features.user.onboarding.OnboardingBridge
import lv.zzdats.commonfeature.features.user.onboarding.OnboardingCoordinator
import lv.zzdats.commonfeature.features.user.onboarding.OnboardingInteractor
import lv.zzdats.commonfeature.features.user.state.AppStateBridge
import lv.zzdats.commonfeature.features.wallet.WalletInteractor
import lv.zzdats.corelogic.controller.AppUpdateStateController
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.dashboardfeature.interactor.DashboardInteractor
import lv.zzdats.dashboardfeature.ui.DashboardBridge
import lv.zzdats.issuancefeature.ui.IssuanceBridge
import lv.zzdats.issuancefeature.ui.document.add.AddDocumentInteractor
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractor
import lv.zzdats.issuancefeature.ui.document.offer.DocumentOfferInteractor
import lv.zzdats.issuancefeature.ui.success.SuccessInteractor
import lv.zzdats.presentationfeature.bridge.PresentationBridge
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.signfeature.bridge.SignBridge
import lv.zzdats.signfeature.interactor.EParakstIdentitiesInteractor
import lv.zzdats.signfeature.interactor.SignDocumentInteractor
import lv.zzdats.signfeature.util.FilePickerHelper
import lv.zzdats.storagelogic.dao.TransactionLogDao
import lv.zzdats.transactionsfeature.TransactionsBridge
import lv.zzdats.transactionsfeature.ui.TransactionsInteractor
import lv.zzdats.uilogic.navigation.WebNavigationService
import lv.zzdats.uilogic.serializer.UiSerializer
import lv.zzdats.webbridge.WebBridge
import lv.zzdats.webbridge.config.WebViewConfig
import lv.zzdats.webbridge.core.WebBridgeInterface
import lv.zzdats.webbridge.registry.BridgeProvider
import lv.zzdats.webbridge.registry.BridgeRegistry
import lv.zzdats.webfeature.bridge.ExternalLinkBridge
import lv.zzdats.webfeature.ui.WebViewModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.zzdats.webfeature")
class WebFeatureModule {

    @Single
    fun provideBridgeProvider(
        navigationService: WebNavigationService,
        successInteractor: SuccessInteractor,
        resourceProvider: ResourceProvider,
        onboardingInteractor: OnboardingInteractor,
        dashboardInteractor: DashboardInteractor,
        documentDetailsInteractor: DocumentDetailsInteractor,
        addDocumentInteractor: AddDocumentInteractor,
        uiSerializer: UiSerializer,
        biometricInteractor: BiometricInteractor,
        documentOfferInteractor: DocumentOfferInteractor,
        walletCoreDocumentsController: WalletCoreDocumentsController,
        deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
        transactionsInteractor: TransactionsInteractor,
        authService: AuthService,
        onboardingCoordinator: OnboardingCoordinator,
        walletInteractor: WalletInteractor,
        prefKeys: PrefKeys,
        signDocumentInteractor: SignDocumentInteractor,
        filePickerHelper: FilePickerHelper,
        eParakstIdentitiesInteractor: EParakstIdentitiesInteractor,
        appUpdateStateController: AppUpdateStateController,
        logController: LogController,
        transactionLogDao: TransactionLogDao,
        configLogic: ConfigLogic,
    ): BridgeProvider = object : BridgeProvider {
        override fun provideBridges(): List<WebBridgeInterface> {
            return listOf(
                AppStateBridge(
                    walletCoreDocumentsController,
                    appUpdateStateController,
                    configLogic.appVersion,
                ),
                OnboardingBridge(
                    onboardingInteractor,
                    navigationService,
                    authService,
                    onboardingCoordinator,
                    deviceAuthenticationInteractor,
                    prefKeys,
                    logController
                ),
                DashboardBridge(
                    dashboardInteractor,
                    documentDetailsInteractor,
                    navigationService,
                    resourceProvider,
                    prefKeys
                ),
                IssuanceBridge(
                    addDocumentInteractor,
                    documentOfferInteractor,
                    navigationService,
                    resourceProvider,
                    successInteractor,
                    uiSerializer,
                    eParakstIdentitiesInteractor,
                    prefKeys,
                    logController
                ),
                SignBridge(
                    signDocumentInteractor,
                    filePickerHelper,
                    navigationService,
                    resourceProvider
                ),
                SettingsBridge(biometricInteractor, resourceProvider, navigationService, deviceAuthenticationInteractor, walletInteractor, prefKeys),
                PresentationBridge(
                    navigationService,
                    resourceProvider,
                    uiSerializer,
                    deviceAuthenticationInteractor,
                    walletCoreDocumentsController,
                    documentDetailsInteractor,
                    prefKeys,
                    transactionLogDao,
                ),
                TransactionsBridge(transactionsInteractor),
                ExternalLinkBridge(
                    navigationService = navigationService
                ),
            )
        }
    }

    @Single
    fun provideWebViewModel(
        bridge: WebBridge,
        onboardingBridge: OnboardingBridge,
        bridgeRegistry: BridgeRegistry,
        webNavigationService: WebNavigationService,
        webViewConfig: WebViewConfig,
    ) = WebViewModel(bridge, onboardingBridge, bridgeRegistry, webNavigationService, webViewConfig)
}
