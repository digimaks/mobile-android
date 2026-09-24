// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.service

import lv.zzdats.businesslogic.util.SecureRandomUtils
import java.util.Base64
import java.security.MessageDigest

object PkceUtils {
    fun generateCodeVerifier(): String {
        val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        val random = SecureRandomUtils.create()
        return (1..64)
            .map { allowed[random.nextInt(allowed.length)] }
            .joinToString("")
    }

    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    fun generateRandomState(): String {
        val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandomUtils.create()
        return (1..32)
            .map { allowed[random.nextInt(allowed.length)] }
            .joinToString("")
    }
}
