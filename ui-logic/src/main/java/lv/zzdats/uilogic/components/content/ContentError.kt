// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import lv.zzdats.resourceslogic.R
import lv.zzdats.uilogic.components.wrap.WrapPrimaryButton

@Composable
internal fun ContentError(config: ContentErrorConfig, paddingValues: PaddingValues) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        ContentTitle(
            title = config.errorTitle ?: stringResource(
                id = R.string.common_error_message
            ),
            subtitle = config.errorSubTitle ?: stringResource(
                id = R.string.common_retry
            ),
            subTitleMaxLines = 10
        )

        Spacer(modifier = Modifier.weight(1f))

        config.onRetry?.let { callback ->
            WrapPrimaryButton(
                onClick = {
                    callback()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.common_retry_button)
                )
            }
        }
    }
}

data class ContentErrorConfig(
    val errorTitle: String? = null,
    val errorSubTitle: String? = null,
    val onCancel: () -> Unit,
    val onRetry: (() -> Unit)? = null
)