// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.security

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import eu.europa.ec.eudi.wallet.EudiWallet
import kotlinx.coroutines.withContext
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import lv.zzdats.businesslogic.config.EnvironmentConfig
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.businesslogic.model.crypto.DeviceIdentifierData
import lv.zzdats.businesslogic.model.crypto.WalletInstanceData
import lv.zzdats.corelogic.util.crypto.toAsn1Der
import lv.zzdats.corelogic.util.securearea.AttestationEncoder.getAttestationCborEncoded
import lv.zzdats.corelogic.util.securearea.ChallengeProcessor
import lv.zzdats.corelogic.util.securearea.ECCPublicKeyHasher
import lv.zzdats.corelogic.util.securearea.KeyAliasGenerator
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.EcSignature
import org.multipaz.securearea.AndroidKeystoreCreateKeySettings
import org.multipaz.securearea.AndroidKeystoreKeyInfo
import org.multipaz.securearea.AndroidKeystoreKeyUnlockData
import org.multipaz.securearea.KeyLockedException
import org.multipaz.securearea.KeyInvalidatedException
import org.multipaz.securearea.KeyUnlockData
import org.multipaz.securearea.KeyUnlockDataProvider
import org.multipaz.securearea.SecureArea
import org.multipaz.securearea.UserAuthenticationType
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.getDefaultKeyUnlockData
import androidx.biometric.BiometricPrompt
import kotlin.time.Duration.Companion.seconds
import org.multipaz.securearea.UnlockReason


interface SecureAreaController {
    /**
     * Generates a new key with attestation.
     *
     * @param challenge A Base64-encoded challenge from the backend.
     * @param keyUnlockData User-authenticated key unlock data; if null, signing will
     * attempt without an authenticated session (and may fail).
     * @param preferStrongBox Whether to prefer StrongBox-backed key generation.
     * @return Attestation payload used to register wallet instance.
     */
    suspend fun generateKeyWithAttestation(
        challenge: String,
        keyUnlockData: AndroidKeystoreKeyUnlockData? = null,
        preferStrongBox: Boolean = true
    ): WalletInstanceData?

    /**
     * Prepares key unlock data for the hardware key, creating the key if it does not exist.
     *
     * @param challenge The backend challenge used when creating the key.
     */
    suspend fun prepareKeyUnlockData(
        challenge: String,
        preferStrongBox: Boolean = true
    ): AndroidKeystoreKeyUnlockData?

    suspend fun getHardwareKeyTag(): String?

    suspend fun checkInstance(): Boolean

    /**
     * Deletes a key from SecureArea.
     *
     * @param alias The alias of the key.
     */
    suspend fun deleteKey()

    /**
     * Whether the hardware key requires user authentication.
     */
    suspend fun isHardwareKeyUserAuthRequired(): Boolean

    /**
     * Returns a CryptoObject for signing with the hardware key (may be null for timeout-based auth).
     */
    suspend fun getHardwareKeyCryptoObjectForSigning(): BiometricPrompt.CryptoObject?

    /**
     * Checks whether the hardware key can be used without prompting (valid auth token present).
     */
    suspend fun isHardwareKeyCurrentlyAuthenticated(): Boolean

    /**
     * Returns attestation CBOR for an already existing key (no key creation).
     *
     * @param challenge A Base64-encoded challenge from the backend used to create the signature.
     * @return CBOR-encoded attestation string or null if unavailable.
     */
    suspend fun getExistingAttestationCbor(challenge: String, alias: String, keyUnlockData: AndroidKeystoreKeyUnlockData? = null): String?

    /**
     * Whether the current hardware key supports device credential (PIN/pattern/password) auth.
     */
    suspend fun isHardwareKeyDeviceCredentialCapable(): Boolean
}

class SecureAreaControllerImpl(
    private val context: Context,
    private val prefKeys: PrefKeys,
    private val secureAreaProvider: SecureAreaProvider,
    private val eudiWallet: Lazy<EudiWallet>,
    private val environmentConfig: EnvironmentConfig,
    private val logController: LogController,
) : SecureAreaController {

    private companion object {
        const val TAG = "SecureAreaController"
    }

    private var secureArea: SecureArea? = null

    private suspend fun ensureSecureAreaInitialized() {
        if (secureArea == null) {
            val wallet = eudiWallet.value
            secureArea = secureAreaProvider.getSecureArea(wallet)
        }
    }

    private fun buildCreateKeySettings(challengeBytes: ByteArray, useStrongBox: Boolean): AndroidKeystoreCreateKeySettings {
        val authPolicy = resolveHardwareKeyAuthPolicy()
        return AndroidKeystoreCreateKeySettings.Builder(ByteString(challengeBytes))
            .setAlgorithm(Algorithm.ESP256)
            .setUserAuthenticationRequired(
                true,
                authPolicy.timeout,
                authPolicy.authTypes
            )
            .setUseStrongBox(useStrongBox)
            .build()
    }

    override suspend fun generateKeyWithAttestation(
        challenge: String,
        keyUnlockData: AndroidKeystoreKeyUnlockData?,
        preferStrongBox: Boolean
    ): WalletInstanceData? {
        return try {
            ensureSecureAreaInitialized()
            val secureAreaInstance = secureArea ?: return null

            val challengeBytes = ChallengeProcessor.decodeBase64ToSha256(challenge)
                ?: run {
                    logController.e(TAG) { "Invalid challenge" }
                    return null
                }

            val alias = ensureHardwareKey(
                secureAreaInstance = secureAreaInstance,
                challengeBytes = challengeBytes,
                preferStrongBox = preferStrongBox
            ) ?: return null

            val keyInfo = secureAreaInstance.getKeyInfo(alias)
            val signature = signWithUnlockData(
                secureArea = secureAreaInstance,
                alias = alias,
                data = challengeBytes,
                keyUnlockData = keyUnlockData
            )
            val attestationCbor = getAttestationCborEncoded(keyInfo, signature)

            val keyHash = ECCPublicKeyHasher.getHash(secureAreaInstance, alias)

            if (attestationCbor.isNullOrEmpty() || keyHash.isNullOrEmpty()) return null

            val manufacturer = Build.MANUFACTURER.ifBlank { "unknown" }
            val model = Build.MODEL.ifBlank { "unknown" }

            WalletInstanceData(
                challenge = challenge,
                keyAttestation = attestationCbor,
                hardwareKeyTag = keyHash,
                deviceIdentifiers = listOf(
                    DeviceIdentifierData(
                        manufacturer = manufacturer,
                        model = model
                    )
                )
            )
        } catch (e: Exception) {
            logController.e(TAG, e)
            null
        }
    }

    override suspend fun prepareKeyUnlockData(
        challenge: String,
        preferStrongBox: Boolean
    ): AndroidKeystoreKeyUnlockData? {
        return try {
            ensureSecureAreaInitialized()
            val secureAreaInstance = secureArea ?: return null
            val challengeBytes = ChallengeProcessor.decodeBase64ToSha256(challenge) ?: return null

            val alias = ensureHardwareKey(
                secureAreaInstance = secureAreaInstance,
                challengeBytes = challengeBytes,
                preferStrongBox = preferStrongBox
            ) ?: return null
            getDefaultKeyUnlockData(secureAreaInstance, alias)
        } catch (e: Exception) {
            logController.e(TAG, e)
            null
        }
    }

    override suspend fun checkInstance(): Boolean {
        return try {
            ensureSecureAreaInitialized()
            val secureAreaInstance = secureArea ?: return false
            val alias = prefKeys.getHardwareKey()

            val keyInfo = secureAreaInstance.getKeyInfo(alias)
            if (!isHardwareKeyCompatible(keyInfo)) {
                logController.w(TAG) { "Hardware key exists but is not compatible with device credential auth" }
                return false
            }
            true
        } catch (e: KeyInvalidatedException) {
            logController.e(TAG, e)
            false
        } catch (e: IllegalArgumentException) {
            logController.e(TAG, e)
            if (prefKeys.getHardwareKey().isNotBlank()) {
                prefKeys.setHardwareKey("")
                prefKeys.setWalletInstanceRegistered(false)
            }
            false
        } catch (e: Exception) {
            logController.e(TAG, e)
            false
        }
    }

    override suspend fun getHardwareKeyTag(): String? {
        return try {
            ensureSecureAreaInitialized()
            val secureAreaInstance = secureArea ?: return null
            val alias = prefKeys.getHardwareKey()
            if (alias.isBlank()) return null
            ECCPublicKeyHasher.getHash(secureAreaInstance, alias)
        } catch (e: Exception) {
            logController.w(TAG) { "Failed to read hardware key tag: ${e.message}" }
            null
        }
    }

    override suspend fun deleteKey() {
        prefKeys.setWalletInstanceRegistered(false)
        try {
            ensureSecureAreaInitialized()
            val secureAreaInstance = secureArea ?: return
            val alias = prefKeys.getHardwareKey()

            secureAreaInstance.getKeyInfo(alias)
            secureAreaInstance.deleteKey(alias)
        } catch (e: IllegalArgumentException) {
            logController.e(TAG, e)
        } catch (e: KeyInvalidatedException) {
            logController.e(TAG, e)
        } catch (e: Exception) {
            logController.e(TAG, e)
        }
    }

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> kotlinx.serialization.json.JsonNull
        is Int -> JsonPrimitive(this)
        is Long -> JsonPrimitive(this)
        is Float -> JsonPrimitive(this)
        is Double -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(this.entries.associate {
            it.key.toString() to it.value.toJsonElement()
        })
        is List<*> -> kotlinx.serialization.json.JsonArray(this.map { it.toJsonElement() })
        else -> JsonPrimitive(this.toString())
    }

    override suspend fun isHardwareKeyUserAuthRequired(): Boolean {
        return try {
            ensureSecureAreaInitialized()
            val secureAreaInstance = secureArea ?: return false
            val alias = prefKeys.getHardwareKey()
            if (alias.isBlank()) return false
            val keyInfo = secureAreaInstance.getKeyInfo(alias)
            (keyInfo as? AndroidKeystoreKeyInfo)?.isUserAuthenticationRequired ?: false
        } catch (e: Exception) {
            logController.w(TAG) { "Failed to read hardware key auth requirement: ${e.message}" }
            false
        }
    }

    override suspend fun getHardwareKeyCryptoObjectForSigning(): BiometricPrompt.CryptoObject? {
        return try {
            ensureSecureAreaInitialized()
            val secureAreaInstance = secureArea ?: return null
            val alias = prefKeys.getHardwareKey()
            if (alias.isBlank()) return null
            val keyUnlockData = getDefaultKeyUnlockData(secureAreaInstance, alias) ?: return null
            keyUnlockData.getCryptoObjectForSigning()
        } catch (e: Exception) {
            logController.w(TAG) { "Failed to create hardware key CryptoObject: ${e.message}" }
            null
        }
    }

    override suspend fun isHardwareKeyCurrentlyAuthenticated(): Boolean {
        return try {
            ensureSecureAreaInitialized()
            val secureAreaInstance = secureArea ?: return false
            val alias = prefKeys.getHardwareKey()
            if (alias.isBlank()) return false
            // Attempt a lightweight sign to verify a valid auth token is present.
            secureAreaInstance.sign(alias, byteArrayOf(0x01), UnlockReason.Unspecified)
            true
        } catch (e: KeyLockedException) {
            false
        } catch (e: Exception) {
            logController.w(TAG) { "Hardware key auth check failed: ${e.message}" }
            false
        }
    }

    override suspend fun isHardwareKeyDeviceCredentialCapable(): Boolean {
        return try {
            ensureSecureAreaInitialized()
            val secureAreaInstance = secureArea ?: return false
            val alias = prefKeys.getHardwareKey()
            if (alias.isBlank()) return false
            val keyInfo = secureAreaInstance.getKeyInfo(alias)
            isHardwareKeyCompatible(keyInfo)
        } catch (e: Exception) {
            logController.w(TAG) { "Failed to inspect hardware key auth types: ${e.message}" }
            false
        }
    }

    private fun base64Url(input: ByteArray): String {
        return android.util.Base64.encodeToString(input, android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE)
    }

    override suspend fun getExistingAttestationCbor(challenge: String, alias: String, keyUnlockData: AndroidKeystoreKeyUnlockData?): String? {
        return try {
            ensureSecureAreaInitialized()
            val secureAreaInstance = secureArea ?: return null

            val challengeBytes = ChallengeProcessor.decodeBase64ToSha256(challenge) ?: return null

            val keyInfo = secureAreaInstance.getKeyInfo(alias)
            val signature = signWithUnlockData(
                secureArea = secureAreaInstance,
                alias = alias,
                data = challengeBytes,
                keyUnlockData = keyUnlockData
            )
            getAttestationCborEncoded(keyInfo, signature)
        } catch (e: Exception) {
            logController.e(TAG, e)
            null
        }
    }

    private suspend fun signWithUnlockData(
        secureArea: SecureArea,
        alias: String,
        data: ByteArray,
        keyUnlockData: KeyUnlockData?
    ): EcSignature {
        if (keyUnlockData == null) {
            return secureArea.sign(alias, data, UnlockReason.Unspecified)
        }

        return withContext(keyUnlockData.asProvider()) {
            secureArea.sign(alias, data, UnlockReason.Unspecified)
        }
    }

    private fun KeyUnlockData.asProvider(): KeyUnlockDataProvider {
        val unlockData = this
        return object : KeyUnlockDataProvider {
            override suspend fun getKeyUnlockData(
                secureArea: SecureArea,
                alias: String,
                unlockReason: UnlockReason
            ): KeyUnlockData = unlockData
        }
    }

    private suspend fun ensureHardwareKey(
        secureAreaInstance: SecureArea,
        challengeBytes: ByteArray,
        preferStrongBox: Boolean
    ): String? {
        val existingAlias = prefKeys.getHardwareKey().takeIf { it.isNotBlank() }
        if (existingAlias != null) {
            runCatching { secureAreaInstance.getKeyInfo(existingAlias) }
                .onSuccess { keyInfo ->
                    if (isHardwareKeyCompatible(keyInfo)) {
                        return existingAlias
                    }
                    logController.w(TAG) { "Existing hardware key auth types are incompatible, recreating key" }
                    runCatching { secureAreaInstance.deleteKey(existingAlias) }
                }
                .onFailure { logController.w(TAG) { "Existing hardware key invalid, recreating: ${it.message}" } }
        }

        val alias = KeyAliasGenerator.generate()
        prefKeys.setHardwareKey(alias)
        prefKeys.setWalletInstanceRegistered(false)

        var created = false
        if (preferStrongBox) {
            runCatching {
                secureAreaInstance.createKey(alias, buildCreateKeySettings(challengeBytes, true))
                created = true
            }.onFailure {
                logController.w(TAG) { "StrongBox creation failed: ${it.message}. Fallback to TEE/software" }
            }
        } else {
            logController.w(TAG) { "StrongBox disabled for this attestation attempt. Creating TEE/software key." }
        }
        if (!created) {
            secureAreaInstance.createKey(alias, buildCreateKeySettings(challengeBytes, false))
        }
        return alias
    }

    private fun isHardwareKeyCompatible(keyInfo: Any): Boolean {
        val androidKeyInfo = keyInfo as? AndroidKeystoreKeyInfo ?: return true
        val authPolicy = resolveHardwareKeyAuthPolicy()
        if (!androidKeyInfo.isUserAuthenticationRequired) return false
        if (androidKeyInfo.userAuthenticationTimeout != authPolicy.timeout) return false
        return androidKeyInfo.userAuthenticationTypes.containsAll(authPolicy.authTypes)
    }

    private fun resolveHardwareKeyAuthPolicy(): HardwareKeyAuthPolicy {
        val biometricManager = BiometricManager.from(context)
        val hasStrongBiometrics =
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
        val hasDeviceCredential =
            biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS

        return if (hasStrongBiometrics) {
            HardwareKeyAuthPolicy(
                timeout = 0.seconds,
                authTypes = setOf(UserAuthenticationType.BIOMETRIC, UserAuthenticationType.LSKF)
            )
        } else if (hasDeviceCredential) {
            HardwareKeyAuthPolicy(
                timeout = 30.seconds,
                authTypes = setOf(UserAuthenticationType.LSKF)
            )
        } else {
            HardwareKeyAuthPolicy(
                timeout = 0.seconds,
                authTypes = setOf(UserAuthenticationType.BIOMETRIC, UserAuthenticationType.LSKF)
            )
        }
    }
}

private data class HardwareKeyAuthPolicy(
    val timeout: kotlin.time.Duration,
    val authTypes: Set<UserAuthenticationType>
)

