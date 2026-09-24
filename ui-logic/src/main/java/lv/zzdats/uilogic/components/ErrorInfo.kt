// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lv.zzdats.uilogic.components.utils.SIZE_SMALL
import lv.zzdats.uilogic.components.wrap.WrapIcon

@Composable
fun ErrorInfo(
    informativeText: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    iconAlpha: Float = 0.4f,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val errorIconSize = (screenWidth / 6).dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SIZE_SMALL.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WrapIcon(
            iconData = AppIcons.Error,
            modifier = Modifier.size(errorIconSize),
            customTint = contentColor,
            contentAlpha = iconAlpha
        )
        Text(
            text = informativeText,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}