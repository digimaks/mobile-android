// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.di

import lv.zzdats.analyticslogic.controller.AnalyticsController
import lv.zzdats.uilogic.navigation.RouterHost
import lv.zzdats.uilogic.navigation.RouterHostImpl
import lv.zzdats.uilogic.navigation.WebNavigationService
import lv.zzdats.uilogic.serializer.UiSerializer
import lv.zzdats.uilogic.serializer.UiSerializerImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.zzdats.uilogic")
class LogicUiModule

@Single
fun provideRouterHost(
    analyticsController: AnalyticsController
): RouterHost = RouterHostImpl(analyticsController)

@Factory
fun provideUiSerializer(): UiSerializer = UiSerializerImpl()

@Single
fun provideWebNavigationService(): WebNavigationService = WebNavigationService()