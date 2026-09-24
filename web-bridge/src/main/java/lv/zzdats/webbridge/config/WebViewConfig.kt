// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.webbridge.config

import android.webkit.WebSettings
import android.content.Context
import android.view.View
import androidx.webkit.WebViewAssetLoader

data class WebViewConfig(
    val domain: String = "appassets.androidplatform.net",
    val assetPath: String = "vue-app/dist",
    val security: SecurityConfig = SecurityConfig(),
    val network: NetworkConfig = NetworkConfig(),
    val ui: UiConfig = UiConfig()
) {
    data class SecurityConfig(
        val javaScriptEnabled: Boolean = true,
        val domStorageEnabled: Boolean = true,
        val allowFileAccess: Boolean = false,
        val allowedDomains: List<String> = emptyList()
    )

    data class NetworkConfig(
        val cacheMode: Int = WebSettings.LOAD_DEFAULT,
        val allowedContentProviders: List<String> = emptyList()
    )

    data class UiConfig(
        val backgroundColor: Int = android.graphics.Color.TRANSPARENT,
        val layerType: Int = View.LAYER_TYPE_HARDWARE,
        val loadWithOverviewMode: Boolean = true,
        val useWideViewPort: Boolean = true,
        val supportZoom: Boolean = false,
        val builtInZoomControls: Boolean = false,
        val displayZoomControls: Boolean = false
    )

    fun getAssetUrl(path: String) = "https://$domain/$assetPath/$assetPath/$path"

    fun createAssetLoader(context: Context) = WebViewAssetLoader.Builder()
        .addPathHandler("/$assetPath/", WebViewAssetLoader.AssetsPathHandler(context))
        .setDomain(domain)
        .build()
}