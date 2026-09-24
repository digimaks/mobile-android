// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.assemblylogic

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import lv.zzdats.businesslogic.controller.NetworkStatusController
import lv.zzdats.businesslogic.security.SecurityProviderGuard
import lv.zzdats.commonfeature.router.featureCommonGraph
import lv.zzdats.corelogic.controller.AppUpdateStateController
import lv.zzdats.startupfeature.router.featureStartupGraph
import lv.zzdats.startupfeature.ui.SplashViewModel
import lv.zzdats.uilogic.DigimaksComponentActivity
import lv.zzdats.webfeature.router.featureWebGraph
import lv.zzdats.signfeature.util.FilePickerHelper
import lv.zzdats.webbridge.WebBridge
import lv.zzdats.webbridge.registry.BridgeRegistry
import lv.zzdats.webfeature.ui.WebViewPool
import lv.zzdats.startupfeature.update.InAppUpdateHelper
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : DigimaksComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val networkStatusController: NetworkStatusController by inject()
    private val viewModel: SplashViewModel by viewModel()
    private val webBridge: WebBridge by inject()
    private val bridgeRegistry: BridgeRegistry by inject()
    private val filePickerHelper: FilePickerHelper by inject()
    private val appUpdateStateController: AppUpdateStateController by inject()
    private val inAppUpdateHelper: InAppUpdateHelper by inject()

    private var webFirstPageReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setKeepOnScreenCondition { !viewModel.isInitialized }
        }
        // TODO: Uncomment
//        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        
        val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            filePickerHelper.handlePickedFiles(uris)
        }
        filePickerHelper.registerPicker(filePickerLauncher)
        
        networkStatusController.startMonitoring(this)

        bridgeRegistry.getAllBridges().forEach { webBridge.registerBridge(it) }
        webBridge.setOnInitialPageLoadedListener {
            webFirstPageReady = true
        }
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            runCatching {
                WebView.setWebContentsDebuggingEnabled(true)
            }.onFailure {
                Log.e(TAG, "Failed to enable WebView debugging", it)
            }
        }
        SecurityProviderGuard.harden("before_webview_prewarm")
        runCatching {
            WebViewPool.prewarm(this, webBridge)
        }.onFailure {
            Log.e(TAG, "Failed to prewarm WebView", it)
        }

        enableEdgeToEdge()
        setContent {
            Content(intent) {
                featureStartupGraph(it)
                featureCommonGraph(it)
                featureWebGraph(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkStatusController.stopMonitoring()
        inAppUpdateHelper.unregister()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            inAppUpdateHelper.resumeIfDownloaded()
        }
    }
}
