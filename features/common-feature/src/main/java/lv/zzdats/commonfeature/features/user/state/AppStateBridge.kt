// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.user.state

import android.animation.ValueAnimator
import android.content.res.Resources
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.launch
import lv.zzdats.commonfeature.BuildConfig
import lv.zzdats.commonfeature.util.extractFullNameFromDocumentOrEmpty
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.controller.AppUpdateStateController
import lv.zzdats.webbridge.core.BaseBridge
import lv.zzdats.webbridge.core.BridgeRequest
import lv.zzdats.webbridge.core.BridgeResponse
import java.util.Locale

class AppStateBridge(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val appUpdateStateController: AppUpdateStateController,
    private val appVersion: String,
) : BaseBridge() {
    override fun getName() = "app"

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when (request.function) {
            "getState" -> handleGetUserInfo(request)
            else -> createErrorResponse(request, "Unknown function")
        }
    }

    private fun handleGetUserInfo(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            val mainPid = walletCoreDocumentsController.getMainPidDocument()
            val language = Locale.getDefault().language
            val scaleFactor = getSystemFontScaleFactor()
            val reduceMotionEnabled = isReduceMotionEnabled()

            val response = mapOf(
                "theme" to "light",
                "language" to language,
                "fullName" to (mainPid?.let { extractFullNameFromDocumentOrEmpty(it) } ?: ""),
                "system" to "android",
                "appVersion" to appVersion,
                "env" to if (BuildConfig.FLAVOR == "prod") "prod" else "dev",
                "updateAvailable" to appUpdateStateController.recommendedUpdateAvailable.value,
                "scaleFactor" to scaleFactor,
                "reduceMotion" to reduceMotionEnabled,
            )

            emitEvent(createSuccessResponse(request, response))
        }
        return createSuccessResponse(request, null)
    }

    private fun getSystemFontScaleFactor(): Double {
        return Resources.getSystem().configuration.fontScale.toDouble()
    }

    private fun isReduceMotionEnabled(): Boolean {
        val context = webView?.context ?: return false
        val resolver = context.contentResolver
        val animatorScale = Settings.Global.getFloat(
            resolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        val transitionScale = Settings.Global.getFloat(
            resolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            1f
        )
        val windowScale = Settings.Global.getFloat(
            resolver,
            Settings.Global.WINDOW_ANIMATION_SCALE,
            1f
        )
        val animationsDisabledBySystemScales =
            animatorScale == 0f && transitionScale == 0f && windowScale == 0f

        val animatorsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            true
        }

        return animationsDisabledBySystemScales || !animatorsEnabled
    }
}
