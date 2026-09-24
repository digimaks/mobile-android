// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.controller.auth

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import lv.zzdats.authlogic.model.BiometricCrypto
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.resourceslogic.R

interface DeviceAuthController {
    fun deviceSupportsBiometrics(listener: (BiometricsAvailability) -> Unit)
    fun authenticate(
        context: Context,
        biometryCrypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        result: DeviceAuthenticationResult,
        policy: DeviceAuthenticationPolicy = DeviceAuthenticationPolicy.DEFAULT
    )

    fun launchBiometricSystemScreen()
}

class DeviceAuthControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val biometricAuthController: BiometricAuthController,
    private val logController: LogController
) : DeviceAuthController {

    private companion object {
        const val TAG = "DeviceAuthController"
    }

    override fun deviceSupportsBiometrics(listener: (BiometricsAvailability) -> Unit) {
        biometricAuthController.deviceSupportsBiometrics(listener)
    }

    override fun authenticate(
        context: Context,
        biometryCrypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        result: DeviceAuthenticationResult,
        policy: DeviceAuthenticationPolicy
    ) {
        val activity = context as? FragmentActivity
        if (activity == null) {
            logController.e(TAG) { "Authentication failed: context is not a FragmentActivity (${context::class.java.name})" }
            result.onAuthenticationError()
            return
        }

        activity.lifecycleScope.launch {
            try {
                val biometricManager = BiometricManager.from(resourceProvider.provideContext())
                val hasStrongBiometrics =
                    biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
                val hasWeakBiometrics =
                    biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
                val hasDeviceCredential =
                    biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
                val allowedAuthenticators = resolveAllowedAuthenticators(
                    hasStrongBiometrics = hasStrongBiometrics,
                    hasWeakBiometrics = hasWeakBiometrics,
                    hasDeviceCredential = hasDeviceCredential,
                    hasCryptoObject = biometryCrypto.cryptoObject != null,
                    policy = policy
                )
                if (allowedAuthenticators == 0) {
                    logController.e(TAG) { "Authentication failed: no compatible authenticators for current API/device" }
                    result.onAuthenticationError()
                    return@launch
                }

                val usesOnlyDeviceCredential =
                    allowedAuthenticators == BiometricManager.Authenticators.DEVICE_CREDENTIAL
                val promptTitle = if (usesOnlyDeviceCredential) {
                    resourceProvider.getString(R.string.device_credential_prompt_title)
                } else {
                    resourceProvider.getString(R.string.biometric_prompt_title)
                }
                val promptSubtitle = if (usesOnlyDeviceCredential) {
                    resourceProvider.getString(R.string.device_credential_prompt_subtitle)
                } else {
                    resourceProvider.getString(R.string.biometric_prompt_subtitle)
                }

                val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(promptTitle)
                    .setSubtitle(promptSubtitle)
                    .setAllowedAuthenticators(allowedAuthenticators)
                if (allowedAuthenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL == 0) {
                    promptInfoBuilder.setNegativeButtonText(resourceProvider.getString(R.string.common_cancel))
                }

                val data = biometricAuthController.authenticate(
                    activity = activity,
                    biometryCrypto = biometryCrypto,
                    promptInfo = promptInfoBuilder.build(),
                    notifyOnAuthenticationFailure = notifyOnAuthenticationFailure
                )

                if (data.authenticationResult != null) {
                    result.onAuthenticationSuccess()
                } else if (
                    data.errorCode == BiometricsAuthError.Cancel.code ||
                    data.errorCode == BiometricsAuthError.CancelByUser.code
                ) {
                    result.onAuthenticationCancelled()
                } else if (data.hasError) {
                    result.onAuthenticationError()
                } else {
                    result.onAuthenticationFailure()
                }
            } catch (error: Exception) {
                logController.e(TAG, error)
                result.onAuthenticationError()
            }
        }
    }

    override fun launchBiometricSystemScreen() {
        biometricAuthController.launchBiometricSystemScreen()
    }

    private fun resolveAllowedAuthenticators(
        hasStrongBiometrics: Boolean,
        hasWeakBiometrics: Boolean,
        hasDeviceCredential: Boolean,
        hasCryptoObject: Boolean,
        policy: DeviceAuthenticationPolicy
    ): Int {
        if (policy == DeviceAuthenticationPolicy.HARDWARE_KEY && !hasCryptoObject) {
            return when {
                hasStrongBiometrics && hasDeviceCredential ->
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                hasStrongBiometrics -> BiometricManager.Authenticators.BIOMETRIC_STRONG
                hasDeviceCredential -> BiometricManager.Authenticators.DEVICE_CREDENTIAL
                else -> 0
            }
        }

        if (hasCryptoObject) {
            // Crypto-bound auth requires strong biometrics; API 29 and below cannot combine
            // device credential with CryptoObject.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return if (hasStrongBiometrics) {
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                } else {
                    0
                }
            }
            return when {
                hasStrongBiometrics && hasDeviceCredential ->
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                hasStrongBiometrics -> BiometricManager.Authenticators.BIOMETRIC_STRONG
                else -> 0
            }
        }
        return when {
            hasStrongBiometrics && hasDeviceCredential ->
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            hasStrongBiometrics -> BiometricManager.Authenticators.BIOMETRIC_STRONG
            hasWeakBiometrics && hasDeviceCredential ->
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            hasWeakBiometrics -> BiometricManager.Authenticators.BIOMETRIC_WEAK
            hasDeviceCredential -> BiometricManager.Authenticators.DEVICE_CREDENTIAL
            else -> 0
        }
    }
}

enum class DeviceAuthenticationPolicy {
    DEFAULT,
    HARDWARE_KEY
}

data class DeviceAuthenticationResult(
    val onAuthenticationSuccess: () -> Unit = {},
    val onAuthenticationCancelled: () -> Unit = {},
    val onAuthenticationError: () -> Unit = {},
    val onAuthenticationFailure: () -> Unit = {},
)
