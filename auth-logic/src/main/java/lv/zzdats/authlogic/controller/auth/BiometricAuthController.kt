// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.controller.auth

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import lv.zzdats.authlogic.controller.storage.BiometryStorageController
import lv.zzdats.authlogic.model.BiometricAuth
import lv.zzdats.authlogic.model.BiometricCrypto
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.businesslogic.controller.crypto.CryptoController
import lv.zzdats.businesslogic.extensions.decodeFromPemBase64String
import lv.zzdats.businesslogic.extensions.encodeToPemBase64String
import lv.zzdats.resourceslogic.provider.ResourceProvider
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import kotlin.coroutines.resume
import lv.zzdats.resourceslogic.R

enum class BiometricsAuthError(val code: Int) {
    Cancel(10), CancelByUser(13)
}

interface BiometricAuthController {
    fun deviceSupportsBiometrics(listener: (BiometricsAvailability) -> Unit)
    fun authenticate(
        context: Context,
        notifyOnAuthenticationFailure: Boolean,
        listener: (BiometricsAuthenticate) -> Unit
    )

    suspend fun authenticate(
        activity: FragmentActivity,
        biometryCrypto: BiometricCrypto,
        promptInfo: BiometricPrompt.PromptInfo,
        notifyOnAuthenticationFailure: Boolean,
    ): BiometricPromptData

    fun launchBiometricSystemScreen()
}

class BiometricAuthControllerImpl(
    private val resourceProvider: ResourceProvider,
    private val cryptoController: CryptoController,
    private val biometryStorageController: BiometryStorageController,
    private val logController: LogController,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BiometricAuthController {

    private companion object {
        const val TAG = "BiometricAuthController"
        const val AUTHENTICATOR_STRONG_OR_CREDENTIAL = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    }

    override fun deviceSupportsBiometrics(listener: (BiometricsAvailability) -> Unit) {
        val biometricManager = BiometricManager.from(resourceProvider.provideContext())
        val capability = readBiometricCapability(biometricManager)

        logController.i(TAG) {
            "Biometric capability check: " +
                    "strongOrCredential=${capability.strongOrCredential.codeName()}, " +
                    "strong=${capability.strong.codeName()}, " +
                    "weak=${capability.weak.codeName()}"
        }

        when {
            capability.hasSuccess() -> {
                listener.invoke(BiometricsAvailability.CanAuthenticate)
            }

            capability.hasNoneEnrolled() -> {
                listener.invoke(BiometricsAvailability.NonEnrolled)
            }

            capability.hasNoHardware() -> {
                listener.invoke(
                    BiometricsAvailability.Failure(resourceProvider.getString(R.string.biometric_no_hardware))
                )
            }

            else -> {
                listener.invoke(BiometricsAvailability.Failure(resourceProvider.getString(R.string.biometric_unknown_error)))
            }
        }
    }

    override fun authenticate(
        context: Context,
        notifyOnAuthenticationFailure: Boolean,
        listener: (BiometricsAuthenticate) -> Unit
    ) {
        val activity = context as? FragmentActivity
        if (activity == null) {
            logController.e(TAG) { "Authentication failed: context is not a FragmentActivity (${context::class.java.name})" }
            listener.invoke(BiometricsAuthenticate.Failed(context.getString(R.string.common_error_description)))
            return
        }

        activity.lifecycleScope.launch {
            try {
                val storedCrypto = retrieveCrypto()
                val biometricData = storedCrypto.first
                val cipher = storedCrypto.second

                if (cipher == null) {
                    listener.invoke(
                        BiometricsAuthenticate.Failed(context.getString(R.string.common_error_description))
                    )
                    return@launch
                }

                val allowedAuthenticators = resolveCryptoPromptAuthenticators()
                val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(activity.getString(R.string.biometric_prompt_title))
                    .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
                    .setAllowedAuthenticators(allowedAuthenticators)
                    .setConfirmationRequired(false)
                if (requiresNegativeButton(allowedAuthenticators)) {
                    promptInfoBuilder.setNegativeButtonText(activity.getString(R.string.common_cancel))
                }

                val data = authenticate(
                    activity = activity,
                    biometryCrypto = BiometricCrypto(BiometricPrompt.CryptoObject(cipher)),
                    promptInfo = promptInfoBuilder.build(),
                    notifyOnAuthenticationFailure = notifyOnAuthenticationFailure
                )

                if (data.authenticationResult != null) {
                    val state = verifyCrypto(
                        context = context,
                        result = data.authenticationResult,
                        biometricAuthentication = biometricData
                    )
                    listener.invoke(state)
                } else if (
                    data.errorCode != BiometricsAuthError.Cancel.code &&
                    data.errorCode != BiometricsAuthError.CancelByUser.code
                ) {
                    logController.e(TAG) {
                        "Biometric prompt error: code=${data.errorCode}, message=${data.errorString}"
                    }
                    listener.invoke(
                        BiometricsAuthenticate.Failed(
                            data.errorString
                                .takeIf { it.isNotBlank() }
                                ?.toString()
                                ?: context.getString(R.string.common_error_description)
                        )
                    )
                } else {
                    listener.invoke(BiometricsAuthenticate.Cancelled)
                }
            } catch (error: Exception) {
                logController.e(TAG, error)
                listener.invoke(BiometricsAuthenticate.Failed(context.getString(R.string.common_error_description)))
            }
        }
    }

    override fun launchBiometricSystemScreen() {
        val enrollIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BIOMETRIC_WEAK
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        enrollIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        resourceProvider.provideContext().startActivity(enrollIntent)
    }

    override suspend fun authenticate(
        activity: FragmentActivity,
        biometryCrypto: BiometricCrypto,
        promptInfo: BiometricPrompt.PromptInfo,
        notifyOnAuthenticationFailure: Boolean
    ): BiometricPromptData = suspendCancellableCoroutine { continuation ->
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) {
                        continuation.resume(
                            BiometricPromptData(
                                authenticationResult = null,
                                errorCode = errorCode,
                                errorString = errString,
                                isPromptDismissed = true
                            )
                        )
                    }
                }

                override fun onAuthenticationSucceeded(result: AuthenticationResult) {
                    if (continuation.isActive) {
                        continuation.resume(
                            BiometricPromptData(
                                authenticationResult = result,
                                isPromptDismissed = true
                            )
                        )
                    }
                }

                override fun onAuthenticationFailed() {
                    // Don't resume here - let the prompt stay active
                    if (continuation.isActive && notifyOnAuthenticationFailure) {
                        // Only notify about failure without dismissing
                        // Commented out to not emit error on in prompt failure
//                        continuation.resume(
//                            BiometricPromptData(
//                                authenticationResult = null,
//                                isPromptDismissed = false
//                            )
//                        )
                    }
                }
            }
        )
        biometryCrypto.cryptoObject?.let {
            prompt.authenticate(promptInfo, it)
        } ?: prompt.authenticate(promptInfo)
    }

    private fun resolveCryptoPromptAuthenticators(): Int {
        val biometricManager = BiometricManager.from(resourceProvider.provideContext())
        val capability = readBiometricCapability(biometricManager)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // CryptoObject + DEVICE_CREDENTIAL is not supported on older Android versions.
            return when {
                capability.strong == BiometricManager.BIOMETRIC_SUCCESS -> BIOMETRIC_STRONG
                capability.weak == BiometricManager.BIOMETRIC_SUCCESS -> BIOMETRIC_WEAK
                else -> AUTHENTICATOR_STRONG_OR_CREDENTIAL
            }
        }
        return when {
            capability.strongOrCredential == BiometricManager.BIOMETRIC_SUCCESS -> AUTHENTICATOR_STRONG_OR_CREDENTIAL
            capability.strong == BiometricManager.BIOMETRIC_SUCCESS -> BIOMETRIC_STRONG
            capability.weak == BiometricManager.BIOMETRIC_SUCCESS -> BIOMETRIC_WEAK
            else -> AUTHENTICATOR_STRONG_OR_CREDENTIAL
        }
    }

    private fun requiresNegativeButton(allowedAuthenticators: Int): Boolean {
        return allowedAuthenticators and DEVICE_CREDENTIAL == 0
    }

    private fun readBiometricCapability(biometricManager: BiometricManager): BiometricCapability {
        return BiometricCapability(
            strongOrCredential = biometricManager.canAuthenticate(AUTHENTICATOR_STRONG_OR_CREDENTIAL),
            strong = biometricManager.canAuthenticate(BIOMETRIC_STRONG),
            weak = biometricManager.canAuthenticate(BIOMETRIC_WEAK),
        )
    }

    private fun Int.codeName(): String = when (this) {
        BiometricManager.BIOMETRIC_SUCCESS -> "BIOMETRIC_SUCCESS"
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "BIOMETRIC_ERROR_NO_HARDWARE"
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "BIOMETRIC_ERROR_HW_UNAVAILABLE"
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "BIOMETRIC_ERROR_NONE_ENROLLED"
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED"
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "BIOMETRIC_ERROR_UNSUPPORTED"
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "BIOMETRIC_STATUS_UNKNOWN"
        else -> "UNKNOWN($this)"
    }

    private suspend fun retrieveCrypto(): Pair<BiometricAuth?, Cipher?> =
        withContext(dispatcher) {
            val biometricData = biometryStorageController.getBiometricAuth()
            val cipher = cryptoController.getBiometricCipher(
                encrypt = biometricData == null,
                ivBytes = biometricData?.ivString?.decodeFromPemBase64String() ?: ByteArray(0)
            )
            Pair(biometricData, cipher)
        }

    private suspend fun verifyCrypto(
        context: Context,
        result: AuthenticationResult?,
        biometricAuthentication: BiometricAuth?
    ): BiometricsAuthenticate = withContext(dispatcher) {
        result?.cryptoObject?.cipher?.let {
            if (biometricAuthentication == null) {
                val randomString = cryptoController.generateCodeVerifier()
                biometryStorageController.setBiometricAuth(
                    BiometricAuth(
                        randomString = randomString,
                        encryptedString = cryptoController.encryptDecryptBiometric(
                            cipher = it,
                            byteArray = randomString.toByteArray(StandardCharsets.UTF_8)
                        ).encodeToPemBase64String().orEmpty(),
                        ivString = it.iv.encodeToPemBase64String().orEmpty()
                    )
                )
                BiometricsAuthenticate.Success
            } else {
                if (biometricAuthentication.randomString
                        .toByteArray(StandardCharsets.UTF_8)
                        .contentEquals(
                            cryptoController.encryptDecryptBiometric(
                                cipher = it,
                                byteArray = biometricAuthentication.encryptedString
                                    .decodeFromPemBase64String() ?: ByteArray(0)
                            )
                        )
                ) {
                    BiometricsAuthenticate.Success
                } else {
                    BiometricsAuthenticate.Failed(context.getString(R.string.common_error_description))
                }
            }
        } ?: BiometricsAuthenticate.Failed(context.getString(R.string.common_error_description))
    }
}

sealed class BiometricsAuthenticate {
    data object Success : BiometricsAuthenticate()
    data class Failed(val errorMessage: String) : BiometricsAuthenticate()
    data object Cancelled : BiometricsAuthenticate()
}

sealed class BiometricsAvailability {
    data object CanAuthenticate : BiometricsAvailability()
    data object NonEnrolled : BiometricsAvailability()
    data class Failure(val errorMessage: String) : BiometricsAvailability()
}

data class BiometricPromptData(
    val authenticationResult: AuthenticationResult?,
    val errorCode: Int = -1,
    val errorString: CharSequence = "",
    val isPromptDismissed: Boolean = false
) {
    val hasError: Boolean get() = errorCode != -1 && isPromptDismissed
}

private data class BiometricCapability(
    val strongOrCredential: Int,
    val strong: Int,
    val weak: Int
) {
    fun hasSuccess(): Boolean {
        return strongOrCredential == BiometricManager.BIOMETRIC_SUCCESS ||
                strong == BiometricManager.BIOMETRIC_SUCCESS ||
                weak == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun hasNoneEnrolled(): Boolean {
        return strongOrCredential == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
                strong == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
                weak == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    fun hasNoHardware(): Boolean {
        return strongOrCredential == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
                strong == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
                weak == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    }
}
