// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.startupfeature.di

import lv.zzdats.authlogic.controller.auth.OnboardingStorageController
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.commonfeature.features.user.onboarding.OnboardingCoordinator
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.controller.AppUpdateStateController
import lv.zzdats.corelogic.security.SecurityInteractor
import lv.zzdats.networklogic.session.SessionManager
import lv.zzdats.networklogic.session.TokenStorage
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.startupfeature.domain.usecase.CheckAppVersionUseCase
import lv.zzdats.startupfeature.interactor.SplashInteractor
import lv.zzdats.startupfeature.interactor.SplashInteractorImpl
import lv.zzdats.uilogic.serializer.UiSerializer
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.zzdats.startupfeature")
class StartupModule

@Factory
fun provideSplashInteractor(
    uiSerializer: UiSerializer,
    resourceProvider: ResourceProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    onboardingCoordinator: OnboardingCoordinator,
    securityInteractor: SecurityInteractor,
    prefKeys: PrefKeys,
    checkAppVersionUseCase: CheckAppVersionUseCase,
    appUpdateStateController: AppUpdateStateController
): SplashInteractor = SplashInteractorImpl(
    uiSerializer,
    resourceProvider,
    walletCoreDocumentsController,
    onboardingCoordinator,
    securityInteractor,
    prefKeys,
    checkAppVersionUseCase,
    appUpdateStateController
)

@Single
fun provideOnboardingCoordinator(
    onboardingStorage: OnboardingStorageController,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    tokenStorage: TokenStorage,
    sessionManager: SessionManager,
    prefKeys: PrefKeys
): OnboardingCoordinator = OnboardingCoordinator(
    walletCoreDocumentsController,
    onboardingStorage,
    tokenStorage,
    sessionManager,
    prefKeys
)
