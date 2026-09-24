// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.util

import java.security.SecureRandom
import java.security.Security

object SecureRandomUtils {

    private const val BOUNCY_CASTLE_PROVIDER = "BC"
    private val preferredAlgorithms = listOf("NativePRNGNonBlocking", "NativePRNG", "SHA1PRNG")

    fun create(): SecureRandom {
        val sha1Providers = Security.getProviders("SecureRandom.SHA1PRNG").orEmpty()
        val nonBouncyCastleProvider = sha1Providers.firstOrNull { it.name != BOUNCY_CASTLE_PROVIDER }
        if (nonBouncyCastleProvider != null) {
            runCatching {
                return SecureRandom.getInstance("SHA1PRNG", nonBouncyCastleProvider)
            }
        }

        preferredAlgorithms.forEach { algorithm ->
            runCatching {
                return SecureRandom.getInstance(algorithm)
            }
        }

        throw IllegalStateException("No SecureRandom implementation available")
    }
}
