// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.resourceslogic.theme.values

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTypography {
    val Typography = Typography(
        bodyLarge = androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        ),
        titleLarge = androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily.Default,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    )
}