// SPDX-License-Identifier: EUPL-1.2

package project.convention.logic

import org.gradle.api.Project

data class EnvironmentConfig(
    val serverApiUrl: String,
    val sessionApiUrl: String,
    val vciIssuerUrl: String,
    val walletApiUrl: String,
    val eidasAuthUrl: String,
    val appLinkAuthCallbackHost: String,
    val eparakstsClientId: String,
)

fun Project.environmentConfig(flavor: AppFlavor): EnvironmentConfig {
    val prefix = flavor.name.uppercase()

    fun value(name: String, fallback: String): String =
        getProperty<String>("${prefix}_$name", "environment.properties")
            ?.takeIf { it.isNotBlank() }
            ?: fallback

    return EnvironmentConfig(
        serverApiUrl = value("SERVER_API_URL", "https://example.invalid/api/1.0/"),
        sessionApiUrl = value("SESSION_API_URL", "https://example.invalid/idauth/api/1.0/"),
        vciIssuerUrl = value("VCI_ISSUER_URL", "https://example.invalid/wallet"),
        walletApiUrl = value("WALLET_API_URL", "https://example.invalid/"),
        eidasAuthUrl = value("EIDAS_AUTH_URL", "https://example.invalid/idauth"),
        appLinkAuthCallbackHost = value("APP_LINK_AUTH_CALLBACK_HOST", "example.invalid"),
        eparakstsClientId = value("EPARAKSTS_CLIENT_ID", "public-placeholder-client-id"),
    )
}
