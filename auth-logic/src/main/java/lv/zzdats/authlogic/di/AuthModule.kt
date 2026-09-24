// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.di

import lv.zzdats.authlogic.config.StorageConfig
import lv.zzdats.authlogic.config.StorageConfigImpl
import lv.zzdats.authlogic.controller.auth.BiometricAuthController
import lv.zzdats.authlogic.controller.auth.BiometricAuthControllerImpl
import lv.zzdats.authlogic.controller.auth.DeviceAuthController
import lv.zzdats.authlogic.controller.auth.DeviceAuthControllerImpl
import lv.zzdats.authlogic.controller.auth.OnboardingStorageController
import lv.zzdats.authlogic.controller.auth.OnboardingStorageControllerImpl
import lv.zzdats.authlogic.controller.storage.BiometryStorageController
import lv.zzdats.authlogic.controller.storage.BiometryStorageControllerImpl
import lv.zzdats.authlogic.controller.storage.PinStorageController
import lv.zzdats.authlogic.controller.storage.PinStorageControllerImpl
import lv.zzdats.authlogic.storage.PrefsBiometryStorageProvider
import lv.zzdats.authlogic.storage.PrefsOnboardingStorageProvider
import lv.zzdats.authlogic.storage.PrefsPinStorageProvider
import lv.zzdats.businesslogic.controller.PrefsController
import lv.zzdats.businesslogic.controller.crypto.CryptoController
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.zzdats.authlogic")
class AuthModule

@Single
fun provideStorageConfig(
    prefsController: PrefsController
): StorageConfig = StorageConfigImpl(
    pinImpl = PrefsPinStorageProvider(prefsController),
    biometryImpl = PrefsBiometryStorageProvider(prefsController),
    onboardingImpl = PrefsOnboardingStorageProvider(prefsController)
)

@Factory
fun provideBiometricAuthController(
    cryptoController: CryptoController,
    biometryStorageController: BiometryStorageController,
    resourceProvider: ResourceProvider,
    logController: LogController
): BiometricAuthController =
    BiometricAuthControllerImpl(
        resourceProvider,
        cryptoController,
        biometryStorageController,
        logController
    )

@Factory
fun provideDeviceAuthController(
    resourceProvider: ResourceProvider,
    biometricAuthController: BiometricAuthController,
    logController: LogController
): DeviceAuthController =
    DeviceAuthControllerImpl(
        resourceProvider,
        biometricAuthController,
        logController
    )

@Factory
fun providePinStorageController(
    storageConfig: StorageConfig
): PinStorageController = PinStorageControllerImpl(storageConfig)

@Factory
fun provideBiometryStorageController(
    storageConfig: StorageConfig
): BiometryStorageController = BiometryStorageControllerImpl(storageConfig)

@Factory
fun provideOnboardingStorageController(
    storageConfig: StorageConfig
) : OnboardingStorageController = OnboardingStorageControllerImpl(storageConfig)
