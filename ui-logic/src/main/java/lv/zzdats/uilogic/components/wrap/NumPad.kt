// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.wrap

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import lv.zzdats.uilogic.components.AppIcons

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumPad(
    onKeyPressed: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackspaceLongPress: () -> Unit,
    onTouchId: (() -> Unit)? = null,
    showTouchId: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val keys: List<String?> = if (showTouchId) {
        listOf(
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            "touchId", "0", "backspace"
        )
    } else {
        listOf(
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            null, "0", "backspace"
        )
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val buttonSize = when {
        screenWidth < 320.dp -> 64.dp  // Small phones
        screenWidth < 400.dp -> 72.dp  // Medium devices
        else -> 80.dp                  // Larger screens
    }

    val spacing = buttonSize * 0.2f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.width(buttonSize * 3 + spacing * 2),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            keys.chunked(3).forEach { rowKeys ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    rowKeys.forEach { key ->
                        if (key == null) {
                            Spacer(modifier = Modifier.size(buttonSize))
                        } else {
                            NumPadButton(
                                key = key,
                                buttonSize = buttonSize,
                                onKeyPressed = onKeyPressed,
                                onBackspace = onBackspace,
                                onBackspaceLongPress = onBackspaceLongPress,
                                onTouchId = onTouchId,
                                haptic = haptic
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumPadButton(
    key: String,
    buttonSize: Dp,
    onKeyPressed: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackspaceLongPress: () -> Unit,
    onTouchId: (() -> Unit)?,
    haptic: HapticFeedback
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(
                if (key == "backspace") {
                    Modifier.combinedClickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBackspace()
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBackspaceLongPress()
                        }
                    )
                } else {
                    Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        when (key) {
                            "touchId" -> onTouchId?.invoke()
                            else -> onKeyPressed(key)
                        }
                    }
                }
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "backspace" -> WrapIcon(
                iconData = AppIcons.Backspace,
                customTint = MaterialTheme.colorScheme.onSurface
            )
            "touchId" -> WrapIcon(
                iconData = AppIcons.TouchId,
                customTint = MaterialTheme.colorScheme.onSurface
            )
            else -> Text(
                text = key,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CircularPinIndicator(
    modifier: Modifier = Modifier,
    length: Int,
    filledCount: Int,
    hasError: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Dynamically adjust size based on screen width
    val circleSize = when {
        screenWidth < 320.dp -> 10.dp  // Small phones
        screenWidth < 400.dp -> 16.dp  // Medium-sized devices
        else -> 24.dp                  // Default large size
    }

    val borderWidth = when {
        screenWidth < 320.dp -> 2.dp   // Thinner border for small screens
        screenWidth < 400.dp -> 3.dp
        else -> 4.dp
    }

    val spacing = when {
        screenWidth < 320.dp -> 8.dp  // Reduce spacing on small screens
        screenWidth < 400.dp -> 12.dp
        else -> 18.dp
    }

    val borderColor = Color(0xFFC1BFB5)
    val fillColor = MaterialTheme.colorScheme.onBackground
    val errorColor = MaterialTheme.colorScheme.error

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(length) { index -> 
            val isFilled = index < filledCount

            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(
                        color = when {
                            hasError -> errorColor
                            isFilled -> fillColor
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        width = borderWidth,
                        color = when {
                            hasError -> errorColor
                            isFilled -> fillColor
                            else -> borderColor
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

