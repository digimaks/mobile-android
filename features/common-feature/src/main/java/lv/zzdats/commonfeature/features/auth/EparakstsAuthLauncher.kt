// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.content.pm.PackageManager
import android.util.Log
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import lv.zzdats.authlogic.service.AuthMethod
import lv.zzdats.commonfeature.BuildConfig

object EparakstsAuthLauncher {
    private const val TAG = "EparakstsAuthLauncher"

    private val EPARAKSTS_SCHEMES = setOf("eparakstsid", "eparakstsid-demo")
    private val EPARAKSTS_PROD_SCHEMES = setOf("eparakstsid")
    private val EPARAKSTS_DEV_SCHEMES = setOf("eparakstsid-demo")
    private val PREFERRED_CUSTOM_TAB_PACKAGES = listOf(
        "com.android.chrome",
        "com.google.android.apps.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.sec.android.app.sbrowser",
        "com.microsoft.emmx"
    )

    fun launch(context: Context, url: String) {
        launchCustomTabs(context, url)
    }

    fun canHandleEparakstsApp(context: Context): Boolean {
        val allowedSchemes = if (BuildConfig.FLAVOR == "prod") {
            EPARAKSTS_PROD_SCHEMES
        } else {
            EPARAKSTS_DEV_SCHEMES
        }

        return allowedSchemes.any { scheme ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$scheme://"))
            val pm = context.packageManager
            val matches = if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            matches.isNotEmpty()
        }
    }

    fun preferredAuthMethod(context: Context): AuthMethod {
        return if (canHandleEparakstsApp(context)) {
            AuthMethod.EPARAKSTS
        } else {
            AuthMethod.EPARAKSTS_CROSS_DEVICE
        }
    }

    fun isEparakstsScheme(scheme: String?): Boolean {
        if (scheme.isNullOrBlank()) return false
        return EPARAKSTS_SCHEMES.any { it.equals(scheme, ignoreCase = true) }
    }

    private fun launchCustomTabs(context: Context, url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        val preferredPackage = CustomTabsClient.getPackageName(context, PREFERRED_CUSTOM_TAB_PACKAGES)
        if (preferredPackage != null) {
            customTabsIntent.intent.setPackage(preferredPackage)
            Log.i(TAG, "Launching auth Custom Tab with package=$preferredPackage")
        } else {
            Log.w(TAG, "No preferred Custom Tabs package found; falling back to default browser")
        }
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (error: Exception) {
            Log.e(TAG, "Failed to launch preferred Custom Tabs package; retrying default browser", error)
            customTabsIntent.intent.setPackage(null)
            customTabsIntent.launchUrl(context, Uri.parse(url))
        }
    }
}
