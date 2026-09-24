// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.applock

import lv.zzdats.businesslogic.controller.PrefsController
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object AppLockManager {
    private const val KEY_LAST_AUTH_AT = "app_lock_last_auth_at"
    private const val KEY_LAST_BG_AT = "app_lock_last_bg_at"

    var timeout: Duration = 1.minutes

    private lateinit var prefs: PrefsController
    private var activationChecker: () -> Boolean = { true }

    fun init(prefsController: PrefsController) {
        if (!this::prefs.isInitialized) {
            prefs = prefsController
        }
    }

    fun setActivationChecker(checker: () -> Boolean) {
        activationChecker = checker
    }

    fun recordAuthenticatedNow(nowMs: Long = System.currentTimeMillis()) {
        ensureInit()
        prefs.setLong(KEY_LAST_AUTH_AT, nowMs)
    }

    fun recordBackgrounded(nowMs: Long = System.currentTimeMillis()) {
        ensureInit()
        prefs.setLong(KEY_LAST_BG_AT, nowMs)
    }

    fun needsReauth(nowMs: Long = System.currentTimeMillis()): Boolean {
        ensureInit()

        if (!activationChecker()) return false

        val lastBg = prefs.getLong(KEY_LAST_BG_AT, 0L)
        if (lastBg == 0L) return false

        val lastAuth = prefs.getLong(KEY_LAST_AUTH_AT, 0L)

        if (lastAuth >= lastBg) return false

        val elapsedInBg = nowMs - lastBg
        return elapsedInBg >= timeout.inWholeMilliseconds
    }

    private fun ensureInit() {
        check(this::prefs.isInitialized) { "AppLockManager not initialized. Call init(context) or init(prefsController) first." }
    }
}