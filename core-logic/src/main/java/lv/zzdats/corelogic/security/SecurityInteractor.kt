// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.security

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.webkit.WebViewCompat
import lv.zzdats.resourceslogic.provider.ResourceProvider
import java.security.KeyStore

interface SecurityInteractor {
    suspend fun validateDeviceSecurity(): SecurityValidation
}

sealed class SecurityValidation {
    data object Valid : SecurityValidation()
    data class Invalid(val reason: SecurityReason) : SecurityValidation()
}

enum class SecurityReason(val code: String) {
    NO_SCREEN_LOCK("SEC_002"),
    DEVICE_ENCRYPTION_DISABLED("SEC_003"),
    UNSAFE_KEYSTORE("SEC_004"),
    WEBVIEW_TOO_OLD("SEC_005"),
    LEGACY_BIOMETRIC_REQUIRED("SEC_006"),
}

class SecurityInteractorImpl(
    private val resourceProvider: ResourceProvider
) : SecurityInteractor {

    private val minWebViewMajor = 110

    override suspend fun validateDeviceSecurity(): SecurityValidation {
        val context = resourceProvider.provideContext()

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguardManager.isDeviceSecure) {
            return SecurityValidation.Invalid(SecurityReason.NO_SCREEN_LOCK)
        }

        // Android 10 and below cannot combine a CryptoObject with the device-credential
        // fallback. The app-lock flow uses a CryptoObject, so entering a device PIN is not
        // supported on these versions.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && !hasEnrolledBiometric(context)) {
            return SecurityValidation.Invalid(SecurityReason.LEGACY_BIOMETRIC_REQUIRED)
        }

        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val encryptionStatus = devicePolicyManager.storageEncryptionStatus
        if (encryptionStatus != DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE &&
            encryptionStatus != DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER &&
            encryptionStatus != DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY) {
            return SecurityValidation.Invalid(SecurityReason.DEVICE_ENCRYPTION_DISABLED)
        }

        if (!isWebViewVersionSupported(context)) {
            return SecurityValidation.Invalid(SecurityReason.WEBVIEW_TOO_OLD)
        }

        if (!isKeystoreSecure()) {
            return SecurityValidation.Invalid(SecurityReason.UNSAFE_KEYSTORE)
        }

        return SecurityValidation.Valid
    }

    private fun isKeystoreSecure(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isWebViewVersionSupported(context: Context): Boolean {
        val versionName = WebViewCompat.getCurrentWebViewPackage(context)?.versionName
        val major = versionName
            ?.substringBefore('.')
            ?.toIntOrNull()
        return major != null && major >= minWebViewMajor
    }

    private fun hasEnrolledBiometric(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS ||
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }
}
