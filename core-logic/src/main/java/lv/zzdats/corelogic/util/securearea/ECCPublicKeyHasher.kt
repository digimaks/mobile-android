// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.util.securearea


import android.util.Base64
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate
import org.multipaz.securearea.SecureArea
import java.security.MessageDigest

object ECCPublicKeyHasher {
    /**
     * Retrieves and hashes the ECC public key.
     *
     * @param secureArea The SecureArea instance.
     * @param alias The alias of the key.
     * @return SHA-256 hash of the public key (Base64 encoded).
     */
    suspend fun getHash(secureArea: SecureArea, alias: String): String? {
        return try {
            val keyInfo = secureArea.getKeyInfo(alias)
            val ecPublicKey = keyInfo.publicKey as? EcPublicKeyDoubleCoordinate ?: return null

            val uncompressedPubKey = byteArrayOf(0x04) + ecPublicKey.x + ecPublicKey.y
            val hash = MessageDigest.getInstance("SHA-256").digest(uncompressedPubKey)

            Base64.encodeToString(hash, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }
}
