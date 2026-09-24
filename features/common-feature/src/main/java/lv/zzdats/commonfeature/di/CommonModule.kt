// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.di

import lv.zzdats.authlogic.controller.auth.BiometricAuthController
import lv.zzdats.authlogic.controller.auth.DeviceAuthController
import lv.zzdats.authlogic.controller.storage.BiometryStorageController
import lv.zzdats.authlogic.service.AuthService
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.businesslogic.controller.PrefsController
import lv.zzdats.businesslogic.controller.crypto.SecureAreaRepository
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractorImpl
import lv.zzdats.commonfeature.features.biometric.BiometricInteractor
import lv.zzdats.commonfeature.features.biometric.BiometricInteractorImpl
import lv.zzdats.commonfeature.features.qr_scan.QrScanInteractor
import lv.zzdats.commonfeature.features.qr_scan.QrScanInteractorImpl
import lv.zzdats.commonfeature.features.user.onboarding.OnboardingBridge
import lv.zzdats.commonfeature.features.user.onboarding.OnboardingCoordinator
import lv.zzdats.commonfeature.features.user.onboarding.OnboardingInteractor
import lv.zzdats.commonfeature.features.wallet.WalletInteractor
import lv.zzdats.commonfeature.features.wallet.WalletInteractorImpl
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.security.SecurityInteractor
import lv.zzdats.corelogic.security.SecurityInteractorImpl
import lv.zzdats.networklogic.api.wallet.WalletApiClient
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.storagelogic.service.DatabaseService
import lv.zzdats.uilogic.navigation.WebNavigationService
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("lv.zzdats.commonfeature")
class CommonModule

@Factory
fun provideBiometricInteractor(
    biometryStorageController: BiometryStorageController,
    biometricAuthenticationController: BiometricAuthController,
): BiometricInteractor {
    return BiometricInteractorImpl(
        biometryStorageController,
        biometricAuthenticationController
    )
}

@Factory
fun provideWalletInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    prefsController: PrefsController,
    databaseService: DatabaseService,
    secureAreaRepository: SecureAreaRepository
) : WalletInteractor {
    return WalletInteractorImpl(walletCoreDocumentsController, prefsController, databaseService, secureAreaRepository)
}

@Factory
fun provideDeviceAuthenticationInteractor(
    deviceAuthenticationController: DeviceAuthController,
    resourceProvider: ResourceProvider,
    walletApiClient: WalletApiClient,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    authService: AuthService
): DeviceAuthenticationInteractor {
    return DeviceAuthenticationInteractorImpl(deviceAuthenticationController, walletApiClient, walletCoreDocumentsController, authService, resourceProvider)
}

@Factory
fun provideQrScanInteractor(): QrScanInteractor {
    return QrScanInteractorImpl()
}

@Factory
fun provideOnboardingBridge(
    onboardingInteractor: OnboardingInteractor,
    navigationService: WebNavigationService,
    authService: AuthService,
    onboardingCoordinator: OnboardingCoordinator,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    prefKeys: PrefKeys,
    logController: LogController
): OnboardingBridge {
    return OnboardingBridge(
        onboardingInteractor,
        navigationService,
        authService,
        onboardingCoordinator,
        deviceAuthenticationInteractor,
        prefKeys,
        logController
    )
}

@Factory
fun provideSecurityInteractor(
    resourceProvider: ResourceProvider
): SecurityInteractor = SecurityInteractorImpl(
    resourceProvider
)
