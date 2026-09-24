// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.assemblylogic

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import eu.europa.ec.eudi.wallet.EudiWallet
import lv.zzdats.analyticslogic.controller.AnalyticsController
import lv.zzdats.assemblylogic.di.setupKoin
import lv.zzdats.businesslogic.security.SecurityProviderGuard
import lv.zzdats.corelogic.config.WalletConfig
import lv.zzdats.corelogic.controller.WalletCoreLogController
import lv.zzdats.corelogic.worker.ReIssuanceWorkManager
import lv.zzdats.corelogic.worker.RevocationWorkManager
import org.koin.android.ext.android.inject

class Application : Application() {

    private val walletConfig: WalletConfig by inject()
    private val analyticsController: AnalyticsController by inject()
    private val walletCoreLogController: WalletCoreLogController by inject()

    override fun onCreate() {
        super.onCreate()
        val preKoinSecurityReport = SecurityProviderGuard.harden("pre_koin")
        setupKoin()
        val preReportingSecurityReport = SecurityProviderGuard.harden("pre_reporting")
        initializeReporting()
        analyticsController.setCustomKeys(preKoinSecurityReport.asCustomKeys("security_provider_pre_koin"))
        analyticsController.setCustomKeys(preReportingSecurityReport.asCustomKeys("security_provider_pre_reporting"))
        initializeEudiWallet()
        val postEudiSecurityReport = SecurityProviderGuard.harden("post_eudi")
        analyticsController.setCustomKeys(postEudiSecurityReport.asCustomKeys("security_provider_post_eudi"))
        initializeRevocationWorkManager()
        initializeReIssuanceWorkManager()
    }

    private fun initializeReporting() {
        analyticsController.initialize(this)
    }

    private fun initializeEudiWallet() {
        EudiWallet(
            applicationContext,
            walletConfig.config
        ) {
            withLogger(walletCoreLogController)
        }
    }

    private fun initializeRevocationWorkManager() {

        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            workerClass = RevocationWorkManager::class.java,
            repeatInterval = walletConfig.revocationInterval,
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RevocationWorkManager.REVOCATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    private fun initializeReIssuanceWorkManager() {
        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            workerClass = ReIssuanceWorkManager::class.java,
            repeatInterval = walletConfig.documentIssuanceConfig.reissuanceRule.backgroundInterval,
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReIssuanceWorkManager.RE_ISSUANCE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}
