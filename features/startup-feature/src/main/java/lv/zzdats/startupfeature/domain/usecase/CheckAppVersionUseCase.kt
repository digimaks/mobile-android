// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.startupfeature.domain.usecase

import lv.zzdats.businesslogic.config.ConfigLogic
import lv.zzdats.networklogic.api.config.VersionApiClient
import lv.zzdats.startupfeature.domain.model.AppUpdateStatus
import org.koin.core.annotation.Factory

@Factory
class CheckAppVersionUseCase(
    private val versionApiClient: VersionApiClient,
    private val configLogic: ConfigLogic
) {

    suspend operator fun invoke(): AppUpdateStatus {
        val result = versionApiClient.getAppVersionConfig()

        val config = result.getOrNull()?.platforms?.android
            ?: return AppUpdateStatus.Error

        val currentVersion = configLogic.appVersion
        val storeUrl = config.storeUrl.orEmpty()

        val minRequired = config.minRequiredVersion
        val latest = config.latestVersion

        // 1. Check for Force Update (Min Required Version)
        if (!minRequired.isNullOrBlank() &&
            isVersionOlder(currentVersion, minRequired)) {
            return AppUpdateStatus.Mandatory(storeUrl)
        }

        // 2. Check for Recommended Update (Latest Version)
        if (!latest.isNullOrBlank() &&
            isVersionOlder(currentVersion, latest)) {
            return AppUpdateStatus.Recommended(
                storeUrl = storeUrl,
                latestVersion = latest
            )
        }

        return AppUpdateStatus.UpToDate
    }

    /**
     * Returns true if [current] is older (smaller) than [target].
     * Compares semantic versions (e.g., "1.0.0" < "1.0.1").
     */
    private fun isVersionOlder(current: String, target: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val targetParts = target.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(currentParts.size, targetParts.size)

        for (i in 0 until length) {
            val c = currentParts.getOrElse(i) { 0 }
            val t = targetParts.getOrElse(i) { 0 }
            if (c < t) return true
            if (c > t) return false
        }
        return false
    }
}