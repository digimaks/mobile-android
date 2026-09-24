// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.webbridge

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.view.WindowInsets
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import lv.zzdats.webbridge.config.WebViewConfig
import lv.zzdats.webbridge.core.BaseBridge
import lv.zzdats.webbridge.core.WebBridgeInterface

interface UrlHandler {
    fun handleUrl(request: WebResourceRequest): Boolean
}

class WebBridge(
    private val config: WebViewConfig
) {
    private val bridges = mutableListOf<WebBridgeInterface>()
    private var initialPageLoaded = false
    private var onInitialPageLoaded: (() -> Unit)? = null
    var onPageFinishedCallback: ((String?) -> Unit)? = null
    var onPageStartedCallback: ((String?) -> Unit)? = null

    fun registerBridge(bridge: WebBridgeInterface) {
        bridges.add(bridge)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun setup(webView: WebView) {
        configureWebView(webView)
        setupAssetLoader(webView)
        attachBridges(webView)
        webView.setBackgroundColor(Color.TRANSPARENT)
        if (webView.url.isNullOrBlank()) {
            loadInitialPage(webView)
        }
    }

    private fun configureWebView(webView: WebView) {
        webView.setBackgroundColor(config.ui.backgroundColor)
        webView.setLayerType(config.ui.layerType, null)

        webView.settings.apply {
            // Security
            javaScriptEnabled = config.security.javaScriptEnabled
            domStorageEnabled = config.security.domStorageEnabled
            allowFileAccess = config.security.allowFileAccess

            // UI
            loadWithOverviewMode = config.ui.loadWithOverviewMode
            useWideViewPort = config.ui.useWideViewPort
            setSupportZoom(config.ui.supportZoom)
            builtInZoomControls = config.ui.builtInZoomControls
            displayZoomControls = config.ui.displayZoomControls
            textZoom = 100

            // Network
            cacheMode = config.network.cacheMode
        }
    }

    private fun setupAssetLoader(webView: WebView) {
        val assetLoader = config.createAssetLoader(webView.context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ) = if (request.url.scheme == "https") {
                assetLoader.shouldInterceptRequest(request.url)
            } else null

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest
            ): Boolean {
                var handled = false
                bridges.forEach { bridge ->
                    if ((bridge as? UrlHandler)?.handleUrl(request) == true) {
                        handled = true
                    }
                }
                return handled
            }

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                super.onPageFinished(view, url)
                onPageFinishedCallback?.invoke(url)
                if (!initialPageLoaded) {
                    initialPageLoaded = true
                    onInitialPageLoaded?.invoke()
                }
                view?.let {
                    injectNativeInsets(it)
                }
            }

            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: android.graphics.Bitmap?
            ) {
                super.onPageStarted(view, url, favicon)
                onPageStartedCallback?.invoke(url)
                view?.let {
                    injectNativeInsets(it)
                }
            }
        }
    }

    fun setOnInitialPageLoadedListener(listener: () -> Unit) {
        onInitialPageLoaded = listener
        if (initialPageLoaded) listener()
    }

    fun isInitialPageLoaded(): Boolean = initialPageLoaded

    private fun attachBridges(webView: WebView) {
        bridges.forEach { bridge ->
            if (bridge is BaseBridge) {
                bridge.attachWebView(webView)
            }
            webView.addJavascriptInterface(bridge, bridge.getName())
        }
    }

    private fun loadInitialPage(webView: WebView) {
        webView.loadUrl(config.getAssetUrl("index.html"))
    }

    private fun injectNativeInsets(webView: WebView) {
        val statusBarHeight = getStatusBarHeightInCssPx(webView)
        val navigationBarHeight = getNavigationBarHeightInCssPx(webView)
        webView.evaluateJavascript(
            """
            document.documentElement.style.setProperty('--native-inset-top', '${statusBarHeight}px');
            document.documentElement.style.setProperty('--native-inset-bottom', '${navigationBarHeight}px');
            """.trimIndent(),
            null
        )
    }

    private fun getStatusBarHeightInCssPx(webView: WebView): Int {
        val statusBarHeightPx = getStatusBarHeight(webView)
        val density = webView.resources.displayMetrics.density
        return (statusBarHeightPx / density).toInt() + 4 // add artificial 4px for extra padding
    }

    private fun getStatusBarHeight(webView: WebView): Int {
        val context = webView.context
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsets = webView.rootWindowInsets
            windowInsets?.getInsets(WindowInsets.Type.statusBars())?.top
                ?: getStatusBarHeightLegacy(context.resources)
        } else {
            getStatusBarHeightLegacy(context.resources)
        }
    }

    private fun getStatusBarHeightLegacy(resources: Resources): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun getNavigationBarHeightInCssPx(webView: WebView): Int {
        val navigationBarHeightPx = getNavigationBarHeight(webView)
        val density = webView.resources.displayMetrics.density
        return (navigationBarHeightPx / density).toInt()
    }

    private fun getNavigationBarHeight(webView: WebView): Int {
        val context = webView.context
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsets = webView.rootWindowInsets
            windowInsets?.getInsets(WindowInsets.Type.navigationBars())?.bottom
                ?: getNavigationBarHeightLegacy(context.resources)
        } else {
            getNavigationBarHeightLegacy(context.resources)
        }
    }

    private fun getNavigationBarHeightLegacy(resources: Resources): Int {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
}
