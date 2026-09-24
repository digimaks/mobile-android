// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.config

import lv.zzdats.businesslogic.BuildConfig

interface ConfigLogic {

    /**
     * Build Type.
     */
    val appBuildType: AppBuildType get() = AppBuildType.getType()

    /**
     * Application Flavor.
     */
    val appFlavor: AppFlavor

    /**
     * Server Environment Configuration.
     */
    val environmentConfig: EnvironmentConfig

    /**
     * Application version.
     */
    val appVersion: String get() = BuildConfig.APP_VERSION
}

enum class AppFlavor {
    LOCAL, DEMO, DEV, STAGING, PROD
}

enum class AppBuildType {
    DEBUG, RELEASE;

    companion object {
        fun getType(): AppBuildType {
            return when (BuildConfig.BUILD_TYPE) {
                "debug" -> DEBUG
                else -> RELEASE
            }
        }
    }
}

abstract class EnvironmentConfig {
    val environment: ServerConfig
        get() {
            return when (AppBuildType.getType()) {
                AppBuildType.DEBUG -> ServerConfig.Debug
                AppBuildType.RELEASE -> ServerConfig.Release
            }
        }

    val connectTimeoutSeconds: Long get() = 60
    val readTimeoutSeconds: Long get() = 60

    abstract fun getServerHost(): String
    abstract fun getSessionApiHost(): String
    abstract fun getWalletApiHost(): String
    abstract fun getVciIssuerUrl(): String

    sealed class ServerConfig {
        data object Debug : ServerConfig()
        data object Release : ServerConfig()
    }
}
