// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import lv.zzdats.uilogic.DigimaksComponentActivity

fun Context.cacheDeepLink(uri: Uri) {
    val intent = Intent().apply {
        data = uri
    }
    (this as? DigimaksComponentActivity)?.cacheDeepLink(intent)
}

fun Context.finish() {
    (this as? DigimaksComponentActivity)?.finish()
}

fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

fun Context.getPendingDeepLink(): Uri? {
    return (this as? DigimaksComponentActivity)?.pendingDeepLink
}

fun Context.consumePendingDeepLink(): Uri? {
    val activity = this as? DigimaksComponentActivity ?: return null
    val deepLink = activity.pendingDeepLink
    activity.pendingDeepLink = null
    return deepLink
}

fun Context.canHandleOpenId4VpDeepLink(): Boolean {
    return (this as? DigimaksComponentActivity)?.canHandleOpenId4VpDeepLink() == true
}

fun Context.openUrl(uri: Uri) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
    }
}
