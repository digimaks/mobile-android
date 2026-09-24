// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.webbridge.core

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import lv.zzdats.networklogic.error.ErrorUtils
import lv.zzdats.webviewfeature.BuildConfig

interface WebBridgeInterface {
    fun getName(): String

    @JavascriptInterface
    fun handleRequest(request: String): String
}

data class BridgeRequest(
    val id: String,
    val function: String,
    val data: Any? = null
)

data class BridgeResponse(
    val id: String,
    val status: Status,
    val data: Any? = null,
    val error: String? = null
) {
    enum class Status { SUCCESS, ERROR }
}

abstract class BaseBridge : WebBridgeInterface {
    protected val coroutineScope = CoroutineScope(Dispatchers.IO)
    protected var webView: WebView? = null
    private val gson = Gson()

    fun attachWebView(webView: WebView) {
        this.webView = webView
    }

    protected fun findHostActivity(): ComponentActivity? {
        var ctx: Context? = webView?.context
        while (ctx is ContextWrapper) {
            if (ctx is ComponentActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    protected fun findHostFragmentActivity(): FragmentActivity? {
        var ctx: Context? = webView?.context
        while (ctx is ContextWrapper) {
            if (ctx is FragmentActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    @JavascriptInterface
    override fun handleRequest(request: String): String =
        try {
            Log.d("BaseBridge", "handleRequest: $request")
            gson.toJson(handleRequest(gson.fromJson(request, BridgeRequest::class.java)))
        } catch (e: Exception) {
            gson.toJson(BridgeResponse(id = "", status = BridgeResponse.Status.ERROR, error = e.message))
        }

    protected fun emitEvent(response: BridgeResponse, onComplete: (() -> Unit)? = null) {
        webView?.post {
            val jsonData = gson.toJson(response)
            Log.d("BaseBridge", "emitEvent: $jsonData")
            webView?.evaluateJavascript(
                """
                window.dispatchEvent(new CustomEvent('lx-embed-response', {
                    detail: $jsonData
                }));
                """.trimIndent()
            ) {
                onComplete?.invoke()
            }
            if (webView == null) {
                onComplete?.invoke()
            }
        }
        if (webView == null) {
            onComplete?.invoke()
        }
    }

    protected abstract fun handleRequest(request: BridgeRequest): BridgeResponse

    protected fun createSuccessResponse(request: BridgeRequest, data: Any?): BridgeResponse {
        return BridgeResponse(
            id = request.id,
            status = BridgeResponse.Status.SUCCESS,
            data = data
        )
    }

    protected fun createErrorResponse(request: BridgeRequest, error: Any?): BridgeResponse {
        val rawError = error?.toString()
        val mappedError = ErrorUtils.extractErrorCode(rawError)
        return BridgeResponse(
            id = request.id,
            status = BridgeResponse.Status.ERROR,
            error = if (BuildConfig.BRIDGE_INCLUDE_RAW_ERRORS) rawError else mappedError,
        )
    }
}
