// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.controller.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import lv.zzdats.businesslogic.controller.PrefKeys
import java.security.KeyStore
import java.util.UUID
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

interface KeystoreController {
    fun retrieveOrGenerateBiometricKey(): SecretKey?
}

class KeystoreControllerImpl(
    private val prefKeys: PrefKeys,
) : KeystoreController {

    companion object {
        private const val STORE_TYPE = "AndroidKeyStore"
    }

    private var androidKeyStore: KeyStore? = null

    init {
        loadKeyStore()
    }

    private fun loadKeyStore() {
        androidKeyStore = KeyStore.getInstance(STORE_TYPE).apply {
            load(null)
        }
    }

    override fun retrieveOrGenerateBiometricKey(): SecretKey? {
        return androidKeyStore?.let {
            val key = prefKeys.getBiometricKey()
            if (key.isEmpty()) {
                val newKey = createPublicKey()
                generateBiometricKey(newKey)
                prefKeys.setBiometricKey(newKey)
                getBiometricKey(it, newKey)
            } else {
                getBiometricKey(it, key)
            }
        }
    }

    private fun generateBiometricKey(key: String) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE_TYPE)
        val builder = KeyGenParameterSpec.Builder(
            key,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above
            builder.setUserAuthenticationParameters(0,KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        } else {
            // Below Android 11
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }

    private fun getBiometricKey(keyStore: KeyStore, key: String): SecretKey {
        keyStore.load(null)
        return keyStore.getKey(key, null) as SecretKey
    }

    private fun createPublicKey(): GUID =
        (UUID.randomUUID().toString() + UUID.randomUUID()
            .toString()).take(CryptoControllerImpl.MAX_GUID_LENGTH)
}