// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.analyticslogic.di

import lv.zzdats.analyticslogic.config.AnalyticsConfig
import lv.zzdats.analyticslogic.controller.AnalyticsController
import lv.zzdats.analyticslogic.controller.AnalyticsControllerImpl
import lv.zzdats.analyticslogic.provider.CrashlyticsProvider
import lv.zzdats.analyticslogic.provider.FirebaseCrashlyticsProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.zzdats.analyticslogic")
class LogicAnalyticsModule

@Single
fun provideAnalyticsConfig(): AnalyticsConfig {
    return try {
        val impl = Class.forName("lv.zzdats.analyticslogic.config.AnalyticsConfigImpl")
        return impl.getDeclaredConstructor().newInstance() as AnalyticsConfig
    } catch (_: Exception) {
        val impl = object : AnalyticsConfig {}
        impl
    }
}

@Single
fun provideAnalyticsController(analyticsConfig: AnalyticsConfig): AnalyticsController =
    AnalyticsControllerImpl(analyticsConfig)

@Single
fun provideCrashlyticsProvider(): CrashlyticsProvider = FirebaseCrashlyticsProvider()