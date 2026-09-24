// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.webfeature.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.webkit.WebView
import lv.zzdats.webbridge.WebBridge
import java.lang.ref.WeakReference

object WebViewPool {
    @Volatile
    private var prewarmed: WeakReference<WebView>? = null

    fun prewarm(context: Context, bridge: WebBridge) {
        prewarmed?.get()?.let { return }

        val themedCtx = ContextThemeWrapper(context, context.theme)

        val wv = WebView(themedCtx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        bridge.setup(wv)
        prewarmed = WeakReference(wv)
    }

    fun take(): WebView? {
        val wv = prewarmed?.get()
        prewarmed = null
        (wv?.parent as? ViewGroup)?.removeView(wv)
        return wv
    }

    fun clear() {
        prewarmed?.get()?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }
        prewarmed = null
    }
}