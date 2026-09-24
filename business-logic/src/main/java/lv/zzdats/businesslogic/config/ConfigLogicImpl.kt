// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.config

import lv.zzdats.businesslogic.BuildConfig

class ConfigLogicImpl : ConfigLogic {
    override val appFlavor: AppFlavor
        get() = try {
            AppFlavor.valueOf(BuildConfig.FLAVOR.uppercase())
        } catch (_: IllegalArgumentException) {
            AppFlavor.DEV
        }

    override val environmentConfig: EnvironmentConfig = BuildConfigEnvironmentConfig()
}

private class BuildConfigEnvironmentConfig : EnvironmentConfig() {
    override fun getServerHost() = BuildConfig.SERVER_API_URL
    override fun getSessionApiHost() = BuildConfig.SESSION_API_URL
    override fun getVciIssuerUrl() = BuildConfig.VCI_ISSUER_URL
    override fun getWalletApiHost() = BuildConfig.WALLET_API_URL
}
