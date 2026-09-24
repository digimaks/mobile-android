// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.resourceslogic.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import lv.zzdats.resourceslogic.theme.values.AppColors
import lv.zzdats.resourceslogic.theme.values.AppShapes
import lv.zzdats.resourceslogic.theme.values.AppTypography

class ThemeManager {
    lateinit var set: ThemeSet
        private set

    @Composable
    fun Theme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit
    ) {
        val lightColorScheme = set.lightColorScheme
        val darkColorScheme = set.darkColorScheme

        val colorScheme = if (darkTheme) darkColorScheme else lightColorScheme

        MaterialTheme(
            colorScheme =  colorScheme,
            typography = set.typo,
            shapes = set.shapes,
            content = content
        )
    }

    companion object {
        private lateinit var instance: ThemeManager

        @Composable
        fun getInstance(): ThemeManager {
            if (!::instance.isInitialized) {
                instance = ThemeManager()
                instance.set = ThemeSet(
                    lightColorScheme = AppColors.LightColorScheme,
                    darkColorScheme = AppColors.DarkColorScheme,
                    typo = AppTypography.Typography,
                    shapes = AppShapes.Shapes,
                    isInDarkMode = isSystemInDarkTheme()
                )
            }
            return instance
        }
    }
}