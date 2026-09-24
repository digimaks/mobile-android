// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.startupfeature.domain.model

sealed interface AppUpdateStatus {
    data object UpToDate : AppUpdateStatus
    data class Recommended(
        val storeUrl: String,
        val latestVersion: String
    ) : AppUpdateStatus
    data class Mandatory(val storeUrl: String) : AppUpdateStatus
    data object Error : AppUpdateStatus
}