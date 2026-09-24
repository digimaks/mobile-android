// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.startupfeature

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleInitializer
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.startup.Initializer
import lv.zzdats.authlogic.applock.AppLockManager
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.businesslogic.controller.PrefKeysImpl
import lv.zzdats.businesslogic.controller.PrefsControllerImpl
import lv.zzdats.commonfeature.features.applock.AppLockGateActivity

class AppLockInitializer : Initializer<Unit> {

    private companion object {
        var isFirstStart = true
    }

    override fun create(context: Context) {
        val prefs = PrefsControllerImpl(context)
        AppLockManager.init(prefs)
        val prefKeys: PrefKeys = PrefKeysImpl(prefs)
        AppLockManager.setActivationChecker { prefKeys.getAppActivated() }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (isFirstStart) {
                    isFirstStart = false
                    return
                }
                if (AppLockManager.needsReauth()) {
                    AppLockGateActivity.launch(context)
                }
            }
            override fun onStop(owner: LifecycleOwner) {
                AppLockManager.recordBackgrounded()
            }
        })
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(ProcessLifecycleInitializer::class.java)
}