// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.provider

interface PinStorageProvider {
    fun retrievePin(): String
    fun setPin(pin: String)
    fun isPinValid(pin: String): Boolean

    fun getFailedAttempts(): Int
    fun incrementFailedAttempts()
    fun resetFailedAttempts()
    fun setTimeoutTimestamp(timestamp: Long)
    fun getTimeoutTimestamp(): Long
    fun isInSecondPhase(): Boolean
    fun setInSecondPhase(isInSecondPhase: Boolean)
}