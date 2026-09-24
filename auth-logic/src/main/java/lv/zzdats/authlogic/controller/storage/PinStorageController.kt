// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.controller.storage

import lv.zzdats.authlogic.config.StorageConfig
import kotlin.math.ceil


interface PinStorageController {
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
    fun getRemainingTimeoutSeconds(): Long
    fun isInTimeout(): Boolean
    fun getRemainingTimeoutMinutes(): Int
    fun validatePinWithSecurityCheck(pin: String): PinValidationResult
}

class PinStorageControllerImpl(private val storageConfig: StorageConfig) : PinStorageController {
    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val TIMEOUT_DURATION_MILLIS = 10 * 60 * 1000L // 10 minutes
    }

    override fun retrievePin(): String = storageConfig.pinStorageProvider.retrievePin()

    override fun setPin(pin: String) {
        storageConfig.pinStorageProvider.setPin(pin)
    }

    override fun isPinValid(pin: String): Boolean = storageConfig.pinStorageProvider.isPinValid(pin)

    override fun validatePinWithSecurityCheck(pin: String): PinValidationResult {
        if (isInTimeout()) {
            val minutes = getRemainingTimeoutMinutes()
            return PinValidationResult.Timeout(minutes.coerceAtLeast(1)) // Ensure we never show 0
        }

        val isValid = isPinValid(pin)
        if (!isValid) {
            incrementFailedAttempts()
            val attempts = getFailedAttempts()

            if (attempts >= MAX_ATTEMPTS) {
                if (isInSecondPhase()) {
                    return PinValidationResult.DeleteWallet
                } else {
                    setInSecondPhase(true)
                    setTimeoutTimestamp(System.currentTimeMillis())
                    resetFailedAttempts()
                    return PinValidationResult.Timeout(10)
                }
            }

            val remainingAttempts = MAX_ATTEMPTS - attempts
            return PinValidationResult.Invalid(remainingAttempts)
        }

        // Reset on success
        resetFailedAttempts()
        setTimeoutTimestamp(0)
        setInSecondPhase(false)
        return PinValidationResult.Valid
    }

    override fun getFailedAttempts(): Int = storageConfig.pinStorageProvider.getFailedAttempts()

    override fun incrementFailedAttempts() {
        storageConfig.pinStorageProvider.incrementFailedAttempts()
    }

    override fun resetFailedAttempts() {
        storageConfig.pinStorageProvider.resetFailedAttempts()
    }

    override fun setTimeoutTimestamp(timestamp: Long) {
        storageConfig.pinStorageProvider.setTimeoutTimestamp(timestamp)
    }

    override fun getTimeoutTimestamp(): Long {
        return storageConfig.pinStorageProvider.getTimeoutTimestamp()
    }

    override fun isInSecondPhase(): Boolean {
        return storageConfig.pinStorageProvider.isInSecondPhase()
    }

    override fun setInSecondPhase(isInSecondPhase: Boolean) {
        storageConfig.pinStorageProvider.setInSecondPhase(isInSecondPhase)
    }

    override fun getRemainingTimeoutSeconds(): Long {
        val timeoutTimestamp = getTimeoutTimestamp()
        if (timeoutTimestamp == 0L) return 0L

        val timeoutDurationMillis = 10 * 60 * 1000L // 10 minutes in milliseconds
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - timeoutTimestamp

        return if (elapsedTime >= timeoutDurationMillis) {
            0L
        } else {
            (timeoutDurationMillis - elapsedTime) / 1000 // Convert to seconds
        }
    }

    override fun getRemainingTimeoutMinutes(): Int {
        val seconds = getRemainingTimeoutSeconds()
        return ceil(seconds / 60.0).toInt()
    }

    override fun isInTimeout(): Boolean {
        return getRemainingTimeoutSeconds() > 0
    }
}

sealed class PinValidationResult {
    data object Valid : PinValidationResult()
    data class Invalid(val remainingAttempts: Int) : PinValidationResult()
    data class Timeout(val remainingMinutes: Int) : PinValidationResult()
    data object DeleteWallet : PinValidationResult()
}