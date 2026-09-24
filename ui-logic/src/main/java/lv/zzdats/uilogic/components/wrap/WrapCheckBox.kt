// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.wrap

import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

data class CheckboxData(
    val isChecked: Boolean,
    val enabled: Boolean = true,
    val onCheckedChange: ((Boolean) -> Unit)?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapCheckbox(
    checkboxData: CheckboxData,
    modifier: Modifier = Modifier,
) {
    // This is needed, otherwise M3 adds unwanted space around CheckBoxes.
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified
    ) {
        Checkbox(
            checked = checkboxData.isChecked,
            onCheckedChange = checkboxData.onCheckedChange,
            modifier = modifier,
            enabled = checkboxData.enabled,
            colors = CheckboxDefaults.colors(
                uncheckedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}