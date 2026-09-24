// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.assemblylogic.di

import android.app.Application
import lv.zzdats.dashboardfeature.di.FeatureDashboardModule
import lv.zzdats.analyticslogic.di.LogicAnalyticsModule
import lv.zzdats.authlogic.di.AuthModule
import lv.zzdats.businesslogic.di.BusinessModule
import lv.zzdats.commonfeature.di.CommonModule
import lv.zzdats.corelogic.di.CoreModule
import lv.zzdats.issuancefeature.di.IssuanceModule
import lv.zzdats.networklogic.di.NetworkModule
import lv.zzdats.presentationfeature.di.FeaturePresentationModule
import lv.zzdats.resourceslogic.di.ResourcesModule
import lv.zzdats.startupfeature.di.StartupModule
import lv.zzdats.storagelogic.di.LogicStorageModule
import lv.zzdats.transactionsfeature.di.FeatureTransactionsModule
import lv.zzdats.signfeature.di.SignModule
import lv.zzdats.uilogic.di.LogicUiModule
import lv.zzdats.webbridge.di.WebBridgeModule
import lv.zzdats.webfeature.di.WebFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.ksp.generated.module
import org.koin.dsl.module
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import lv.zzdats.startupfeature.update.InAppUpdateHelper

private val assembledModules = listOf(
    // Project modules
    LogicAnalyticsModule().module,
    LogicUiModule().module,
    AuthModule().module,
    BusinessModule().module,
    CoreModule().module,
    ResourcesModule().module,
    LogicStorageModule().module,
    FeatureTransactionsModule().module,
    WebBridgeModule().module,
    StartupModule().module,
    CommonModule().module,
    NetworkModule().module,
    SignModule().module,
    // WebView modules
    WebFeatureModule().module,
    IssuanceModule().module,
    FeatureDashboardModule().module,
    FeaturePresentationModule().module,
    provideAssemblyRuntimeModule(),
)

private fun provideAssemblyRuntimeModule() = module {
    single<AppUpdateManager> { AppUpdateManagerFactory.create(androidContext()) }
    single { InAppUpdateHelper(get()) }
}

internal fun Application.setupKoin() {
    startKoin {
        androidContext(this@setupKoin)
        if (lv.zzdats.assemblylogic.BuildConfig.DEBUG) {
            androidLogger()
        }
        modules(assembledModules)
    }
}
