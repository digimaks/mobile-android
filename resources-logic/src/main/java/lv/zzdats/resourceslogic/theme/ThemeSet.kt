// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.resourceslogic.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography

data class ThemeSet(
    val isInDarkMode: Boolean,
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme,
    val typo: Typography,
    val shapes: Shapes
)