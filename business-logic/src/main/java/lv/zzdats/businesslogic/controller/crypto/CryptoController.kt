// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.controller.crypto

import android.util.Base64
import lv.zzdats.businesslogic.util.SecureRandomUtils
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

typealias GUID = String

interface CryptoController {
    fun generateCodeVerifier(): String
    fun getBiometricCipher(encrypt: Boolean = false, ivBytes: ByteArray? = null): Cipher?
    fun encryptDecryptBiometric(cipher: Cipher?, byteArray: ByteArray): ByteArray
}

class CryptoControllerImpl(
    private val keystoreController: KeystoreController
) : CryptoController {

    companion object {
        private const val AES_EXTERNAL_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 128
        const val MAX_GUID_LENGTH = 64
    }

    override fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandomUtils.create().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    override fun getBiometricCipher(encrypt: Boolean, ivBytes: ByteArray?): Cipher? {
        return try {
            Cipher.getInstance(AES_EXTERNAL_TRANSFORMATION).apply {
                if (encrypt) {
                    init(
                        Cipher.ENCRYPT_MODE,
                        keystoreController.retrieveOrGenerateBiometricKey()
                    )
                } else {
                    init(
                        Cipher.DECRYPT_MODE,
                        keystoreController.retrieveOrGenerateBiometricKey(),
                        GCMParameterSpec(IV_SIZE, ivBytes)
                    )
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun encryptDecryptBiometric(cipher: Cipher?, byteArray: ByteArray): ByteArray {
        return cipher?.doFinal(byteArray) ?: ByteArray(0)
    }
}
