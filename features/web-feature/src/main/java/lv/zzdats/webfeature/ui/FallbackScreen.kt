// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.webfeature.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import lv.zzdats.uilogic.components.AppIcons
import lv.zzdats.uilogic.components.content.ContentTitle
import lv.zzdats.uilogic.components.utils.SPACING_LARGE
import lv.zzdats.uilogic.components.utils.VSpacer
import lv.zzdats.uilogic.components.wrap.WrapIcon
import lv.zzdats.uilogic.components.wrap.WrapPrimaryButton
import lv.zzdats.resourceslogic.R

@Composable
fun FallbackScreen(
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SPACING_LARGE.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WrapIcon(
            iconData = AppIcons.Error,
            customTint = MaterialTheme.colorScheme.error
        )

        VSpacer.Large()

        ContentTitle(
            title = stringResource(id = R.string.web_error_title),
            subtitle = stringResource(id = R.string.web_error_subtitle),
        )

        VSpacer.Large()

        WrapPrimaryButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.web_error_retry))
        }
    }
}