// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.snackbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Custom snackbar options
 */
data class SnackbarConfig(
    val message: String,
    val actionLabel: String? = null,
    val duration: Long = 4000L,
    val style: SnackbarStyle = SnackbarStyle.DEFAULT,
    val onAction: (() -> Unit)? = null
)

enum class SnackbarStyle {
    DEFAULT,
    SUCCESS,
    ERROR
}

object Snackbar {
    @Composable
    fun PlaceHolder(
        snackbarHostState: SnackbarHostState,
        modifier: Modifier = Modifier,
        alignment: Alignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = alignment
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = modifier.padding(16.dp)
            ) { snackbarData ->
                CustomSnackbar(
                    message = snackbarData.visuals.message,
                    actionLabel = snackbarData.visuals.actionLabel,
                    style = SnackbarStyle.DEFAULT,
                    onActionClick = {
                        snackbarData.performAction()
                    }
                )
            }
        }
    }

    suspend fun show(
        snackbarHostState: SnackbarHostState,
        config: SnackbarConfig
    ) {
        val result = snackbarHostState.showSnackbar(
            message = config.message,
            actionLabel = config.actionLabel,
            duration = if(config.duration >= 4000L) {
                androidx.compose.material3.SnackbarDuration.Long
            } else {
                androidx.compose.material3.SnackbarDuration.Short
            }
        )

        when(result) {
            SnackbarResult.ActionPerformed -> config.onAction?.invoke()
            SnackbarResult.Dismissed -> { /* Handle dismiss if needed */ }
        }
    }
}

@Composable
private fun CustomSnackbar(
    message: String,
    actionLabel: String? = null,
    style: SnackbarStyle = SnackbarStyle.DEFAULT,
    onActionClick: () -> Unit = {}
) {
    val elevation = remember { Animatable(0f) }
    val animationSpec: AnimationSpec<Float> = remember {
        tween(durationMillis = 250, easing = FastOutSlowInEasing)
    }

    LaunchedEffect(Unit) {
        elevation.animateTo(6f, animationSpec)
    }

    androidx.compose.material3.Snackbar(
        modifier = Modifier.padding(12.dp),
        shape = MaterialTheme.shapes.small,
        containerColor = when(style) {
            SnackbarStyle.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
            SnackbarStyle.ERROR -> MaterialTheme.colorScheme.errorContainer
            SnackbarStyle.DEFAULT -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = when(style) {
            SnackbarStyle.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
            SnackbarStyle.ERROR -> MaterialTheme.colorScheme.onErrorContainer
            SnackbarStyle.DEFAULT -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        action = actionLabel?.let { label ->
            {
                androidx.compose.material3.TextButton(onClick = onActionClick) {
                    androidx.compose.material3.Text(
                        text = label,
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
        },
        dismissAction = null
    ) {
        androidx.compose.material3.Text(text = message)
    }
}