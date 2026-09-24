// SPDX-License-Identifier: EUPL-1.2

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import project.convention.logic.AppFlavor
import project.convention.logic.addConfigField
import project.convention.logic.environmentConfig
import project.convention.logic.config.LibraryModule
import project.convention.logic.config.LibraryPluginConfig
import project.convention.logic.configureFlavors
import project.convention.logic.configureKotlinAndroid
import project.convention.logic.libs

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        with(target) {
            val config =
                extensions.create<LibraryPluginConfig>("moduleConfig", LibraryModule.Unspecified)

            val walletScheme = "digimaks"
            val walletHost = "*"

            val eudiOpenId4VpScheme = "openid-vp"
            val eudiOpenid4VpHost = "*"

            val mdocOpenId4VpScheme = "mdoc-openid4vp"
            val mdocOpenid4VpHost = "*"

            val openId4VpScheme = "openid4vp"
            val openid4VpHost = "*"

            val haipOpenId4VpScheme = "haip-vp"
            val haipOpenid4VpHost = "*"

            val credentialOfferScheme = "openid-credential-offer"
            val credentialOfferHost = "*"

            val credentialOfferHaipScheme = "haip-vci"
            val credentialOfferHaipHost = "*"

            val openId4VciAuthorizationScheme = "eu.europa.ec.euidi"
            val openId4VciAuthorizationHost = "authorization"
            val appLinkAuthCallbackPath = "/auth-done"


            with(pluginManager) {
                apply("com.android.library")
                apply("project.android.library.kover")
                apply("project.android.lint")
                apply("project.android.koin")
                apply("org.jetbrains.kotlin.android")
                apply("kotlinx-serialization")
                apply("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
                apply("kotlin-parcelize")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                with(defaultConfig) {

                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

                    addConfigField("DEEPLINK", "$walletScheme://")
                    addConfigField("EUDI_OPENID4VP_SCHEME", eudiOpenId4VpScheme)
                    addConfigField("MDOC_OPENID4VP_SCHEME", mdocOpenId4VpScheme)
                    addConfigField("OPENID4VP_SCHEME", openId4VpScheme)
                    addConfigField("HAIP_OPENID4VP_SCHEME", haipOpenId4VpScheme)
                    addConfigField("CREDENTIAL_OFFER_SCHEME", credentialOfferScheme)
                    addConfigField("CREDENTIAL_OFFER_HAIP_SCHEME", credentialOfferHaipScheme)
                    addConfigField("ISSUE_AUTHORIZATION_SCHEME", openId4VciAuthorizationScheme)
                    addConfigField("ISSUE_AUTHORIZATION_HOST", openId4VciAuthorizationHost)
                    addConfigField(
                        "ISSUE_AUTHORIZATION_DEEPLINK",
                        "$openId4VciAuthorizationScheme://$openId4VciAuthorizationHost"
                    )
                    addConfigField("SIGN_FILE_SHARE_SCHEME", "fileshare")
                    addConfigField("FILE_SHARE_HOST", "*")

                    addConfigField("EPARAKSTS_REDIRECT_URI", "$walletScheme://auth-done")
                    addConfigField("APP_LINK_AUTH_CALLBACK_PATH", appLinkAuthCallbackPath)
                    addConfigField("EPARAKSTS_ACR_VALUES", "eparaksts:mobileid")
                    addConfigField("EPARAKSTS_CROSS_DEVICE_ACR_VALUES", "eparaksts:mobileid:cross_device")
                    addConfigField("SMARTID_ACR_VALUES", "smartid")

                    // Manifest placeholders for Wallet deepLink
                    manifestPlaceholders["deepLinkScheme"] = walletScheme
                    manifestPlaceholders["deepLinkHost"] = walletHost
                    manifestPlaceholders["appLinkAuthCallbackPath"] = appLinkAuthCallbackPath

                    // Manifest placeholders used for OpenId4VP
                    manifestPlaceholders["eudiOpenid4vpScheme"] = eudiOpenId4VpScheme
                    manifestPlaceholders["eudiOpenid4vpHost"] = eudiOpenid4VpHost
                    manifestPlaceholders["mdocOpenid4vpScheme"] = mdocOpenId4VpScheme
                    manifestPlaceholders["mdocOpenid4vpHost"] = mdocOpenid4VpHost
                    manifestPlaceholders["openid4vpScheme"] = openId4VpScheme
                    manifestPlaceholders["openid4vpHost"] = openid4VpHost
                    manifestPlaceholders["haipOpenid4vpScheme"] = haipOpenId4VpScheme
                    manifestPlaceholders["haipOpenid4vpHost"] = haipOpenid4VpHost

                    // Manifest placeholders used for OpenId4VCI
                    manifestPlaceholders["credentialOfferHost"] = credentialOfferHost
                    manifestPlaceholders["credentialOfferScheme"] = credentialOfferScheme
                    manifestPlaceholders["credentialOfferHaipHost"] = credentialOfferHaipHost
                    manifestPlaceholders["credentialOfferHaipScheme"] = credentialOfferHaipScheme

                    // Manifest placeholders used for OpenId4VCI Authorization
                    manifestPlaceholders["openId4VciAuthorizationScheme"] =
                        openId4VciAuthorizationScheme
                    manifestPlaceholders["openId4VciAuthorizationHost"] =
                        openId4VciAuthorizationHost
                }

                configureFlavors(this) { flavor ->
                    val environment = target.environmentConfig(flavor)

                    addConfigField("SERVER_API_URL", environment.serverApiUrl)
                    addConfigField("SESSION_API_URL", environment.sessionApiUrl)
                    addConfigField("VCI_ISSUER_URL", environment.vciIssuerUrl)
                    addConfigField("WALLET_API_URL", environment.walletApiUrl)
                    addConfigField("EIDAS_AUTH_URL", environment.eidasAuthUrl)
                    addConfigField(
                        "APP_LINK_AUTH_CALLBACK_HOST",
                        environment.appLinkAuthCallbackHost
                    )
                    addConfigField("EPARAKSTS_CLIENT_ID", environment.eparakstsClientId)
                    addConfigField("BRIDGE_INCLUDE_RAW_ERRORS", flavor != AppFlavor.Prod)
                    manifestPlaceholders["appLinkAuthCallbackHost"] =
                        environment.appLinkAuthCallbackHost
                }

                testOptions {
                    unitTests {
                        isIncludeAndroidResources = true
                        isReturnDefaultValues = true
                    }
                }
            }
            dependencies {
                add("implementation", libs.findLibrary("kotlinx-coroutines-android").get())
                add("implementation", libs.findLibrary("kotlinx-coroutines-guava").get())
                add("implementation", libs.findLibrary("androidx-work-ktx").get())

                add("testImplementation", "junit:junit:4.13.2")
                add("androidTestImplementation", "androidx.test.ext:junit:1.1.5")
            }
            afterEvaluate {
                if (!config.module.isLogicModule && !config.module.isFeatureCommon) {
                    dependencies {
                        add("implementation", project(LibraryModule.CommonFeature.path))
                    }
                }
            }
        }
    }
}
