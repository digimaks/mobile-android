// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun SystemBroadcastReceiver(
    actions: List<String>,
    onEvent: (intent: Intent?) -> Unit
) {
    val context = LocalContext.current

    // If either context or Action changes, unregister and register again
    DisposableEffect(context, actions) {
        val intentFilter = IntentFilter().apply {
            actions.forEach {
                addAction(it)
            }
        }
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onEvent(intent)
            }
        }

        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // When the effect leaves the Composition, remove the callback
        onDispose {
            context.unregisterReceiver(broadcastReceiver)
        }
    }
}