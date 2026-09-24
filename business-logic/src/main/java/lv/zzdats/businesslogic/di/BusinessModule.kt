// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.di

import android.content.Context
import lv.zzdats.analyticslogic.provider.CrashlyticsProvider
import lv.zzdats.businesslogic.config.ConfigLogic
import lv.zzdats.businesslogic.config.ConfigLogicImpl
import lv.zzdats.businesslogic.config.EnvironmentConfig
import lv.zzdats.businesslogic.controller.NetworkStatusController
import lv.zzdats.businesslogic.controller.NetworkStatusControllerImpl
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.businesslogic.controller.PrefKeysImpl
import lv.zzdats.businesslogic.controller.PrefsController
import lv.zzdats.businesslogic.controller.PrefsControllerImpl
import lv.zzdats.businesslogic.controller.crypto.CryptoController
import lv.zzdats.businesslogic.controller.crypto.CryptoControllerImpl
import lv.zzdats.businesslogic.controller.crypto.KeystoreController
import lv.zzdats.businesslogic.controller.crypto.KeystoreControllerImpl
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.businesslogic.controller.log.LogControllerImpl
import lv.zzdats.businesslogic.provider.AppInstanceIdProvider
import lv.zzdats.businesslogic.provider.AppInstanceIdProviderImpl
import lv.zzdats.businesslogic.provider.UuidProvider
import lv.zzdats.businesslogic.provider.UuidProviderImpl
import lv.zzdats.businesslogic.validator.FormValidator
import lv.zzdats.businesslogic.validator.FormValidatorImpl
import lv.zzdats.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.zzdats.businesslogic")
class BusinessModule

@Single
fun providePrefsController(context: Context): PrefsController =
    PrefsControllerImpl(context)

@Single
fun providePrefKeys(prefsController: PrefsController): PrefKeys =
    PrefKeysImpl(prefsController)

@Single
fun provideKeystoreController(
    prefKeys: PrefKeys,
): KeystoreController =
    KeystoreControllerImpl(prefKeys)

@Factory
fun provideCryptoController(keystoreController: KeystoreController): CryptoController =
    CryptoControllerImpl(keystoreController)

@Single
fun provideConfigLogic(): ConfigLogic = ConfigLogicImpl()

@Single
fun provideLogController(context: Context, configLogic: ConfigLogic, crashlyticsProvider: CrashlyticsProvider): LogController =
    LogControllerImpl(context, configLogic, crashlyticsProvider)

@Single
fun provideNetworkStatusController(): NetworkStatusController {
    return NetworkStatusControllerImpl()
}

@Factory
fun provideFormValidator(logController: LogController): FormValidator =
    FormValidatorImpl(logController)

@Single
fun provideEnvironmentConfig(configLogic: ConfigLogic): EnvironmentConfig =
    configLogic.environmentConfig

@Single
fun provideUuidProvider(): UuidProvider {
    return UuidProviderImpl()
}

@Single
fun provideAppInstanceIdProvider(
    prefKeys: PrefKeys,
    uuidProvider: UuidProvider
): AppInstanceIdProvider {
    return AppInstanceIdProviderImpl(prefKeys, uuidProvider)
}
