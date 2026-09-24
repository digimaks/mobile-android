// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.webfeature.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import lv.zzdats.corelogic.util.CoreActions
import lv.zzdats.uilogic.components.SystemBroadcastReceiver
import lv.zzdats.uilogic.components.content.ContentScreen
import lv.zzdats.uilogic.components.content.ScreenNavigateAction
import lv.zzdats.uilogic.extension.canHandleOpenId4VpDeepLink
import lv.zzdats.uilogic.extension.consumePendingDeepLink
import lv.zzdats.uilogic.extension.getPendingDeepLink
import lv.zzdats.uilogic.navigation.DeepLinkType
import lv.zzdats.uilogic.navigation.hasDeepLink
import lv.zzdats.webfeature.BuildConfig

@Composable
fun WebScreen(
    navController: NavController,
    viewModel: WebViewModel,
) {
    val context = LocalContext.current
    val state = viewModel.viewState.value
    var webView by remember { mutableStateOf<WebView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val path = currentBackStackEntry?.arguments?.getString("path")
    val status = currentBackStackEntry?.arguments?.getString("status")

    val backgroundColor = MaterialTheme.colorScheme.background
    val backgroundColorInt = backgroundColor.toArgb()

    val isInternal by viewModel.isInternalPage.collectAsState()

    StatusBarConfig()

    SystemBroadcastReceiver(
        actions = listOf(
            CoreActions.VCI_RESUME_ACTION,
            CoreActions.VCI_DYNAMIC_PRESENTATION,
            CoreActions.EPARAKSTS_AUTH_DONE,
            CoreActions.EPARAKSTS_AUTH_ERROR,
            CoreActions.EPARAKSTS_RESUME,
            CoreActions.SIGN_COMPLETE,
            CoreActions.PENDING_DEEPLINK
        )
    ) { intent ->
        when (intent?.action) {
            CoreActions.VCI_RESUME_ACTION -> {
                intent.extras?.getString("uri")?.let { uri ->
                    webView?.evaluateJavascript("""
                        window.dispatchEvent(new CustomEvent('lx-vciresume-response', { 
                            detail: { uri: '$uri' }
                        }));
                    """.trimIndent(), null)
                }
            }
            CoreActions.EPARAKSTS_AUTH_DONE -> {
                val code = intent?.getStringExtra("code")
                val state = intent?.getStringExtra("state")
                if (code != null && state != null) {
                    viewModel.handleAuthCode(code, state)
                }
            }
            CoreActions.EPARAKSTS_AUTH_ERROR -> {
                intent?.getStringExtra("error")?.let { error ->
                    webView.loadTrackedUrl(viewModel.getWebUrl("index.html#/document-offer-manual/error"), viewModel)
                }
            }
            CoreActions.EPARAKSTS_RESUME -> {
                intent.getStringExtra("url")?.let { url ->
                    webView.loadTrackedUrl(viewModel.getWebUrl("index.html#/$path"), viewModel)
                }
            }
            CoreActions.SIGN_COMPLETE -> {
                val success = intent.getBooleanExtra("success", false)
                val path = if (success) "sign-done/success" else "sign/error"
                webView.loadTrackedUrl(viewModel.getWebUrl("index.html#/$path"), viewModel)
            }
            CoreActions.PENDING_DEEPLINK -> {
                val uri = intent.getStringExtra("uri")?.toUri()
                uri?.let { deepLinkUri ->
                    val deeplinkScheme = BuildConfig.DEEPLINK.removeSuffix("://")
                    val deeplinkUrl = deepLinkUri.toString()
                    val isSignDeepLink = deepLinkUri.scheme == deeplinkScheme &&
                            (deeplinkUrl.contains("eseal-success")
                                    || deeplinkUrl.contains("eseal-error")
                                    || deeplinkUrl.contains("resume_sign"))
                    if (isSignDeepLink) {
                        viewModel.handleSignDeepLink(deepLinkUri)
                        context.consumePendingDeepLink()
                        return@SystemBroadcastReceiver
                    }
                    val action = hasDeepLink(deepLinkUri)
                    if (action?.type != DeepLinkType.OPENID4VP || context.canHandleOpenId4VpDeepLink()) {
                        viewModel.handleDeepLink(deepLinkUri)
                        context.consumePendingDeepLink()
                    }
                }
            }
        }
    }
        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.NONE
        ) { padding ->
            if (state.webViewError) {
                FallbackScreen(
                    onRetry = { viewModel.setEvent(WebEvent.RetryLoading) }
                )
            } else {
                val systemBarsTop = WindowInsets.systemBars.asPaddingValues()
                    .calculateTopPadding()
                val externalTopPadding = if (systemBarsTop > 0.dp) {
                    systemBarsTop
                } else {
                    with(LocalDensity.current) { getStatusBarHeightPx(context).toDp() }
                }
                val externalBottomPadding = WindowInsets.systemBars
                    .only(WindowInsetsSides.Bottom)
                    .asPaddingValues()
                    .calculateBottomPadding()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
//                        .windowInsetsPadding(
//                            WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
//                        )
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                bottom = if (isInternal) 0.dp else externalBottomPadding,
                                top = if (isInternal) 0.dp else externalTopPadding,
                            ),
                        factory = { context ->
                            val pooled = WebViewPool.take()
                            if (pooled != null) {
                                pooled.setBackgroundColor(backgroundColorInt)
                                webView = pooled
                                updateInternalPageState(viewModel, pooled.url)
                                viewModel.setEvent(WebEvent.WebViewCreated(pooled))
                                pooled
                            } else {
                                WebView(context).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setBackgroundColor(backgroundColorInt)
                                    webView = this
                                    updateInternalPageState(viewModel, url)
                                    viewModel.setEvent(WebEvent.WebViewCreated(this))
                                }
                            }
                        }
                    )
                }
            }
        }

    fun Context?.findComponentActivity(): ComponentActivity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is ComponentActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun WebView.currentRoute(): String =
        url?.substringAfter("#/")?.substringBefore("?")?.substringBefore("&").orEmpty()

    val rootRoutes = remember { setOf("", "dashboard", "home") }
    var lastRootBackPressAt by remember { mutableStateOf(0L) }

    BackHandler(enabled = webView != null) {
        fun finishHost() {
            val activity = webView?.context.findComponentActivity()
                ?: (lifecycleOwner as? ComponentActivity)
            activity?.runOnUiThread {
                activity.finish()
            }
        }
        val routeBeforeBack = webView?.currentRoute().orEmpty()

        webView?.evaluateJavascript(
            """
                (function() {
                    const evt = new CustomEvent('lx-native-back', { cancelable: true });
                    window.dispatchEvent(evt);
                    return evt.defaultPrevented ? 'handled' : 'unhandled';
                })();
            """.trimIndent()
        ) { result ->
            webView?.let { view ->
                val isHandledByWeb = result?.contains("handled") == true
                val routeChangedByWeb = view.currentRoute() != routeBeforeBack
                val isRootRoute = view.currentRoute() in rootRoutes

                if (!isHandledByWeb && routeChangedByWeb) {
                    return@evaluateJavascript
                }

                if (isRootRoute) {
                    if (!isHandledByWeb) {
                        finishHost()
                        return@evaluateJavascript
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastRootBackPressAt <= 1500L) {
                        finishHost()
                    } else {
                        lastRootBackPressAt = now
                    }
                    return@evaluateJavascript
                }

                if (isHandledByWeb) return@evaluateJavascript

                if (view.canGoBack()) {
                    view.goBack()
                } else {
                    finishHost()
                }
            } ?: finishHost()
        }
    }

    LaunchedEffect(path, status) {
        path?.let {
            val fullPath = if (status != null) "$it?status=$status" else it
            webView.loadTrackedUrl(viewModel.getWebUrl("index.html#/$fullPath"), viewModel)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WebEffect.Navigation.SwitchScreen -> {
                    navController.navigate(effect.screenRoute)
                }
                is WebEffect.Navigation.LoadUrl -> {
//                    webView?.evaluateJavascript("""
//                            window.dispatchEvent(new CustomEvent('lx-navigation', {
//                                detail: { path: '${effect.path}', params: '${effect.params}', query: '${effect.query}' }
//                            }));
//                        """.trimIndent(), null)
                    webView.loadTrackedUrl(viewModel.getWebUrl("index.html#/${effect.path}"), viewModel)
                }
                is WebEffect.Navigation.LoadExternalUrl -> {
                    val intent = Intent(Intent.ACTION_VIEW, effect.url.toUri())
                    context.startActivity(intent)
                }
                is WebEffect.Navigation.Back -> {
                    webView?.let { view ->
                        if (view.canGoBack()) {
                            view.goBack()
                        } else {
                            (lifecycleOwner as? ComponentActivity)?.finish()
                        }
                    }
                }
                is WebEffect.Reload -> {
                    webView.reloadTracked(viewModel)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                webView?.destroy()
            }
            if(event == Lifecycle.Event.ON_RESUME) {
                Log.d("WebScreen", "onResume called ${webView?.url}")
                if (webView?.url?.contains("eparaksts") == true) {
                    webView.reloadTracked(viewModel)
                }
                webView?.evaluateJavascript("""
                        window.dispatchEvent(new CustomEvent('resume', { 
                            detail: { }
                        }));
                    """.trimIndent(), null)

                webView?.context?.getPendingDeepLink()?.let { deepLinkUri ->
                    val action = hasDeepLink(deepLinkUri)
                    if (action?.type != DeepLinkType.OPENID4VP || context.canHandleOpenId4VpDeepLink()) {
                        viewModel.handleDeepLink(deepLinkUri)
                        webView?.context?.consumePendingDeepLink()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentBackStackEntry?.destination?.route) {
        val deepLinkUri = context.getPendingDeepLink()
        if (deepLinkUri != null) {
            val action = hasDeepLink(deepLinkUri)
            if (action?.type != DeepLinkType.OPENID4VP || context.canHandleOpenId4VpDeepLink()) {
                viewModel.handleDeepLink(deepLinkUri)
                context.consumePendingDeepLink()
            }
        }
    }

}

private fun updateInternalPageState(viewModel: WebViewModel, url: String?) {
    viewModel.setIsInternalPage(viewModel.isInternalUrl(url))
}

private fun WebView?.loadTrackedUrl(url: String, viewModel: WebViewModel) {
    this?.let {
        updateInternalPageState(viewModel, url)
        it.loadUrl(url)
    }
}

private fun WebView?.reloadTracked(viewModel: WebViewModel) {
    this?.let {
        updateInternalPageState(viewModel, it.url)
        it.reload()
    }
}

private fun getStatusBarHeightPx(context: Context): Int {
    val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
}

@Composable
private fun StatusBarConfig() {
    val activity = LocalView.current.context as? Activity
    val window = activity?.window
    val isDarkTheme = isSystemInDarkTheme()

    DisposableEffect(isDarkTheme) {
        val originalColor = window?.statusBarColor

        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowInsetsControllerCompat(it, it.decorView).apply {
                // Set status bar icons dark in light theme, light in dark theme
                isAppearanceLightStatusBars = !isDarkTheme
                isAppearanceLightNavigationBars = !isDarkTheme
            }
        }

        onDispose {
            window?.statusBarColor = originalColor ?: 0
        }
    }
}
