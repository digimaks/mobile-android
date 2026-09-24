// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.startupfeature.update

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.tasks.await

class InAppUpdateHelper(
    private val appUpdateManager: AppUpdateManager
) {

    private var listenerRegistered = false
    private var updateFlowLaunched = false

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            appUpdateManager.completeUpdate()
        }
    }

    suspend fun startUpdate(
        activity: Activity,
        type: Int = AppUpdateType.FLEXIBLE
    ): Boolean {
        if (updateFlowLaunched) return true

        val appUpdateInfo = runCatching { appUpdateManager.appUpdateInfo.await() }.getOrNull()
            ?: return false

        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
            appUpdateManager.completeUpdate()
            return true
        }

        val canUpdate = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            appUpdateInfo.isUpdateTypeAllowed(type)

        if (!canUpdate) return false

        if (type == AppUpdateType.FLEXIBLE) {
            registerListenerIfNeeded()
        }

        val options = AppUpdateOptions.newBuilder(type).build()
        val launched = runCatching {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                options,
                REQUEST_CODE
            )
        }.getOrDefault(false)

        updateFlowLaunched = launched

        if (!launched) {
            unregister()
        }

        return launched
    }

    suspend fun resumeIfDownloaded() {
        val appUpdateInfo = runCatching { appUpdateManager.appUpdateInfo.await() }.getOrNull()
            ?: return

        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
            appUpdateManager.completeUpdate()
        }
    }

    fun unregister() {
        if (listenerRegistered) {
            appUpdateManager.unregisterListener(installStateListener)
            listenerRegistered = false
        }
        updateFlowLaunched = false
    }

    private fun registerListenerIfNeeded() {
        if (!listenerRegistered) {
            appUpdateManager.registerListener(installStateListener)
            listenerRegistered = true
        }
    }

    companion object {
        private const val REQUEST_CODE = 9011
    }
}
