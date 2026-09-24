// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.security

import java.security.SecureRandom
import java.security.Security

object SecurityProviderGuard {

    private const val BOUNCY_CASTLE_PROVIDER = "BC"
    private const val APP_BOUNCY_CASTLE_PACKAGE = "org.bouncycastle."

    data class Report(
        val stage: String,
        val changed: Boolean,
        val providersBefore: String,
        val providersAfter: String,
        val secureRandomBefore: String,
        val secureRandomAfter: String,
        val error: String?,
    ) {
        fun asCustomKeys(prefix: String): Map<String, String> = mapOf(
            "${prefix}_stage" to stage,
            "${prefix}_changed" to changed.toString(),
            "${prefix}_providers_before" to providersBefore,
            "${prefix}_providers_after" to providersAfter,
            "${prefix}_random_before" to secureRandomBefore,
            "${prefix}_random_after" to secureRandomAfter,
            "${prefix}_error" to (error ?: "none"),
        )
    }

    fun harden(stage: String): Report {
        val providersBefore = providersSummary()
        val secureRandomBefore = secureRandomServicesSummary()
        var changed = false
        var error: String? = null

        runCatching {
            val provider = Security.getProvider(BOUNCY_CASTLE_PROVIDER)
            if (provider?.javaClass?.name?.startsWith(APP_BOUNCY_CASTLE_PACKAGE) == true) {
                Security.removeProvider(BOUNCY_CASTLE_PROVIDER)
                Security.addProvider(provider)
                changed = true
            }
        }.onFailure {
            error = it.summary()
        }

        val providersAfter = providersSummary()
        val secureRandomAfter = secureRandomSummary()

        return Report(
            stage = stage,
            changed = changed,
            providersBefore = providersBefore,
            providersAfter = providersAfter,
            secureRandomBefore = secureRandomBefore,
            secureRandomAfter = secureRandomAfter,
            error = error,
        )
    }

    private fun providersSummary(): String =
        Security.getProviders().joinToString(separator = " > ") { provider ->
            "${provider.name}:${provider.javaClass.name}"
        }.take(MAX_VALUE_LENGTH)

    private fun secureRandomServicesSummary(): String =
        Security.getProviders().flatMap { provider ->
            provider.services
                .filter { service -> service.type == SECURE_RANDOM_SERVICE_TYPE }
                .map { service -> "${service.algorithm}:${provider.name}:${service.className}" }
        }.joinToString(separator = " | ")
            .ifBlank { "none" }
            .take(MAX_VALUE_LENGTH)

    private fun secureRandomSummary(): String =
        runCatching {
            val random = SecureRandom()
            "${random.algorithm}:${random.provider.name}:${random.provider.javaClass.name}"
        }.getOrElse {
            it.summary()
        }.take(MAX_VALUE_LENGTH)

    private fun Throwable.summary(): String =
        "${javaClass.simpleName}:${message.orEmpty()}".take(MAX_VALUE_LENGTH)

    private const val MAX_VALUE_LENGTH = 900
    private const val SECURE_RANDOM_SERVICE_TYPE = "SecureRandom"
}
