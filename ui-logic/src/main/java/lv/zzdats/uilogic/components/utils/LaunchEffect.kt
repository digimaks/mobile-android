// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

@Composable
fun OneTimeLaunchedEffect(
    block: () -> Unit
) {
    var initialEffects by rememberSaveable { mutableStateOf(false) }
    if (!initialEffects) {
        LaunchedEffect(Unit) {
            initialEffects = true
            block()
        }
    }
}