// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

enum class TopSpacing {
    WithToolbar, WithoutToolbar
}

fun screenPaddings(
    append: PaddingValues? = null,
    topSpacing: TopSpacing = TopSpacing.WithToolbar
) = PaddingValues(
    start = SPACING_LARGE.dp,
    top = calculateTopSpacing(topSpacing).dp + (append?.calculateTopPadding() ?: 0.dp),
    end = SPACING_LARGE.dp,
    bottom = SPACING_LARGE.dp + (append?.calculateBottomPadding() ?: 0.dp)
)

private fun calculateTopSpacing(topSpacing: TopSpacing): Int = when (topSpacing) {
    TopSpacing.WithToolbar -> SPACING_SMALL
    TopSpacing.WithoutToolbar -> SPACING_EXTRA_LARGE
}