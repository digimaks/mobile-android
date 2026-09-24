// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.resourceslogic.theme.values

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

object AppColors {

    val LightColorScheme = ColorScheme(
        // Primary colors
        primary = Color(0xFF8232F7),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFf5efff),
        onPrimaryContainer = Color(0xFF001E2F),

        // Secondary colors
        secondary = Color(0xFF008752),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDFF5E8),
        onSecondaryContainer = Color(0xFF00391D),

        // Tertiary colors
        tertiary = Color(0xFFE6E1E6),
        onTertiary = Color(0xFF49454F),
        tertiaryContainer = Color(0xFFF7F5F7),
        onTertiaryContainer = Color(0xFF1D192B),

        // Background colors
        background = Color(0xFFf5efff),
        onBackground = Color(0xFF1D1D1F),

        // Surface colors
        surface = Color(0xFFE3E0DA),
        onSurface = Color(0xFF1D1D1F),
        surfaceVariant = Color(0xFFEAE6E1),
        onSurfaceVariant = Color(0xFF49454F),

        // Error colors
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),

        // Additional colors
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        scrim = Color(0xFF000000),
        surfaceTint = Color(0xFF004AD7),
        inverseSurface = Color(0xFF313033),
        inverseOnSurface = Color(0xFFF4EFF4),
        inversePrimary = Color(0xFFCCE5FF)
    )

    val DarkColorScheme = ColorScheme(
        primary = Color(0xFFBA74FF), // --color-brand
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF18151c), // --color-background
        onPrimaryContainer = Color(0xFFCCE5FF),

        secondary = Color(0xFFe3dfd6), // --color-data
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF7b7b6c), // --color-chrome
        onSecondaryContainer = Color(0xFFDFF5E8),

        tertiary = Color(0xFF74aeff), // --color-interactive-background
        onTertiary = Color(0xFF5c5a4e), // --color-interactive-secondary-background
        tertiaryContainer = Color(0xFF3c3936), // --color-region
        onTertiaryContainer = Color(0xFFe3dfd6), // --color-label

        background = Color(0xFF18151d), // --color-background
        onBackground = Color(0xFFe3dfd6), // --color-label

        surface = Color(0xFF3c3936), // --color-region
        onSurface = Color(0xFFe3dfd6), // --color-label
        surfaceVariant = Color(0xFF514e4b), // --color-region-2
        onSurfaceVariant = Color(0xFFCAC4D0),

        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),

        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        scrim = Color(0xFF000000),
        surfaceTint = Color(0xFF74aeff),
        inverseSurface = Color(0xFFE6E1E5),
        inverseOnSurface = Color(0xFF1C1B1F),
        inversePrimary = Color(0xFFCCE5FF)
    )

    val success = Color(0xFF008752)
    val textPrimaryDark = Color(0xFF1D1D1F)
}