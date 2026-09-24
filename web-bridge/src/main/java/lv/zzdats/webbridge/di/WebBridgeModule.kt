// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.webbridge.di

import lv.zzdats.webbridge.WebBridge
import lv.zzdats.webbridge.config.WebViewConfig
import lv.zzdats.webbridge.registry.BridgeProvider
import lv.zzdats.webbridge.registry.BridgeRegistry
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class WebBridgeModule {
    @Single
    fun provideWebViewConfig() = WebViewConfig()

    @Single
    fun provideWebBridge(config: WebViewConfig) = WebBridge(config)

    @Single
    fun provideBridgeRegistry(
        bridgeProviders: List<BridgeProvider>
    ) = BridgeRegistry(bridgeProviders)
}