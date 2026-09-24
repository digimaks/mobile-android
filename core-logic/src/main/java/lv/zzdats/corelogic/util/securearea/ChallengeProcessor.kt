// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.util.securearea

import android.util.Base64
import java.security.MessageDigest

object ChallengeProcessor {
    /**
     * Decodes a Base64-encoded challenge and returns its SHA-256 hash.
     *
     * @param base64String The Base64 challenge.
     * @return The SHA-256 hash or null if decoding fails.
     */
    fun decodeBase64ToSha256(base64String: String): ByteArray? {
        return try {
            val decodedBytes = Base64.decode(
                base64String, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
            MessageDigest.getInstance("SHA-256").digest(decodedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
