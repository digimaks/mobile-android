// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.wrap

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lv.zzdats.uilogic.components.utils.VSpacer

@Composable
fun VisibleCodeInput(
    modifier: Modifier = Modifier,
    onCodeChange: (String) -> Unit,
    length: Int,
    hasError: Boolean = false,
    errorMessage: String? = null
) {
    val code = rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until length) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .border(
                            width = 1.dp,
                            color = if (hasError)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (i < code.value.length) code.value[i].toString() else "_",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (hasError)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        if (hasError && !errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        VSpacer.ExtraLarge()

        NumPad(
            onKeyPressed = { digit ->
                if (code.value.length < length) {
                    code.value = code.value + digit
                    onCodeChange(code.value)
                }
            },
            onBackspaceLongPress = {
                code.value = ""
                onCodeChange(code.value)
            },
            onBackspace = {
                if (code.value.isNotEmpty()) {
                    code.value = code.value.dropLast(1)
                    onCodeChange(code.value)
                }
            }
        )
    }
}