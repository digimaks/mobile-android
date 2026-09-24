// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.wrap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import lv.zzdats.uilogic.components.utils.SIZE_MEDIUM
import lv.zzdats.uilogic.extension.throttledClickable

@Composable
fun WrapCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    throttleClicks: Boolean = true,
    shape: Shape? = null,
    colors: CardColors? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = shape ?: RoundedCornerShape(SIZE_MEDIUM.dp)
    val cardColors = colors ?: CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
    )
    val cardModifier = Modifier
        .clip(cardShape)
        .then(modifier)
        .then(
            if (enabled && onClick != null) {
                when (throttleClicks) {
                    true -> Modifier.throttledClickable {
                        onClick()
                    }

                    false -> Modifier.clickable {
                        onClick()
                    }
                }
            } else Modifier.clickable(enabled = false, onClick = {})
        )

    Card(
        modifier = cardModifier,
        shape = cardShape,
        colors = cardColors
    ) {
        content()
    }
}
