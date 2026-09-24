// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.loading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import lv.zzdats.uilogic.components.utils.Z_LOADING
import lv.zzdats.uilogic.extension.clickableNoRipple

@Composable
fun LoadingIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(Z_LOADING)
            .clickableNoRipple { }
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}