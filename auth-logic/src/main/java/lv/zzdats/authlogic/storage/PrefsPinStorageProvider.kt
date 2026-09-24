// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.storage

import lv.zzdats.authlogic.provider.PinStorageProvider
import lv.zzdats.businesslogic.controller.PrefsController

private object PinPrefsKeys {
    const val DEVICE_PIN = "device_pin"
    const val FAILED_ATTEMPTS = "failed_pin_attempts"
    const val TIMEOUT_TIMESTAMP = "pin_timeout_timestamp"
    const val IN_SECOND_PHASE = "pin_in_second_phase"
}

class PrefsPinStorageProvider(
    private val prefsController: PrefsController
) : PinStorageProvider {

    override fun retrievePin(): String {
        return prefsController.getString(PinPrefsKeys.DEVICE_PIN, "")
    }

    override fun setPin(pin: String) {
        prefsController.setString(PinPrefsKeys.DEVICE_PIN, pin)
    }

    override fun isPinValid(pin: String): Boolean = retrievePin() == pin

    override fun getFailedAttempts(): Int {
        return prefsController.getInt(PinPrefsKeys.FAILED_ATTEMPTS, 0)
    }

    override fun incrementFailedAttempts() {
        val current = getFailedAttempts()
        prefsController.setInt(PinPrefsKeys.FAILED_ATTEMPTS, current + 1)
    }

    override fun resetFailedAttempts() {
        prefsController.setInt(PinPrefsKeys.FAILED_ATTEMPTS, 0)
    }

    override fun setTimeoutTimestamp(timestamp: Long) {
        prefsController.setLong(PinPrefsKeys.TIMEOUT_TIMESTAMP, timestamp)
    }

    override fun getTimeoutTimestamp(): Long {
        return prefsController.getLong(PinPrefsKeys.TIMEOUT_TIMESTAMP, 0L)
    }

    override fun isInSecondPhase(): Boolean {
        return prefsController.getBool(PinPrefsKeys.IN_SECOND_PHASE, false)
    }

    override fun setInSecondPhase(isInSecondPhase: Boolean) {
        prefsController.setBool(PinPrefsKeys.IN_SECOND_PHASE, isInSecondPhase)
    }
}