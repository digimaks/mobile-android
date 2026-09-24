// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.components.content

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import lv.zzdats.uilogic.components.AppIcons
import lv.zzdats.uilogic.components.IconData
import lv.zzdats.uilogic.components.loading.LoadingIndicator
import lv.zzdats.uilogic.components.snackbar.Snackbar
import lv.zzdats.uilogic.components.utils.ALPHA_DISABLED
import lv.zzdats.uilogic.components.utils.ALPHA_ENABLED
import lv.zzdats.uilogic.components.utils.MAX_TOOLBAR_ACTIONS
import lv.zzdats.uilogic.components.utils.TopSpacing
import lv.zzdats.uilogic.components.utils.Z_STICKY
import lv.zzdats.uilogic.components.utils.screenPaddings
import lv.zzdats.uilogic.components.wrap.WrapIcon
import lv.zzdats.uilogic.components.wrap.WrapIconButton

enum class LoadingType {
    NORMAL, NONE
}

data class ToolbarAction(
    val icon: IconData,
    val order: Int = 100,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

data class ToolbarConfig(
    val title: String = "",
    val actions: List<ToolbarAction> = listOf()
)

enum class ScreenNavigateAction {
    BACKABLE, CANCELABLE, NONE
}

@Composable
fun ContentScreen(
    isLoading: Boolean = false,
    toolBarConfig: ToolbarConfig? = null,
    navigatableAction: ScreenNavigateAction = ScreenNavigateAction.BACKABLE,
    onBack: (() -> Unit)? = null,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    stickyBottom: @Composable (() -> Unit)? = null,
    fab: @Composable () -> Unit = {},
    fabPosition: FabPosition = FabPosition.End,
    contentErrorConfig: ContentErrorConfig? = null,
    bodyContent: @Composable (PaddingValues) -> Unit
) {
    ContentScreen(
        loadingType = if (isLoading) LoadingType.NORMAL else LoadingType.NONE,
        toolBarConfig = toolBarConfig,
        navigatableAction = navigatableAction,
        onBack = onBack,
        topBar = topBar,
        bottomBar = bottomBar,
        stickyBottom = stickyBottom,
        fab = fab,
        fabPosition = fabPosition,
        contentErrorConfig = contentErrorConfig,
        bodyContent = bodyContent
    )
}

@Composable
fun ContentScreen(
    loadingType: LoadingType = LoadingType.NONE,
    toolBarConfig: ToolbarConfig? = null,
    navigatableAction: ScreenNavigateAction = ScreenNavigateAction.BACKABLE,
    onBack: (() -> Unit)? = null,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    stickyBottom: @Composable (() -> Unit)? = null,
    fab: @Composable () -> Unit = {},
    fabPosition: FabPosition = FabPosition.End,
    contentErrorConfig: ContentErrorConfig? = null,
    bodyContent: @Composable (PaddingValues) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val hasToolBar = contentErrorConfig != null
            || navigatableAction != ScreenNavigateAction.NONE
            || topBar != null
    val topSpacing = if (hasToolBar) TopSpacing.WithToolbar else TopSpacing.WithoutToolbar

    Scaffold(
        topBar = {
            if (topBar != null && contentErrorConfig == null) topBar.invoke()
            else if (hasToolBar) {
                DefaultToolBar(
                    navigatableAction = contentErrorConfig?.let {
                        ScreenNavigateAction.CANCELABLE
                    } ?: navigatableAction,
                    onBack = contentErrorConfig?.onCancel ?: onBack,
                    keyboardController = keyboardController,
                    toolbarConfig = toolBarConfig,
                )
            }
        },
        bottomBar = bottomBar ?: {},
        floatingActionButton = fab,
        floatingActionButtonPosition = fabPosition,
        snackbarHost = {
            Snackbar.PlaceHolder(snackbarHostState = snackbarHostState)
        }
    ) { padding ->

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (contentErrorConfig != null) {
                ContentError(
                    config = contentErrorConfig,
                    paddingValues = screenPaddings(padding)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    Box(modifier = Modifier.weight(1f)) {
                        bodyContent(screenPaddings(padding, topSpacing))
                    }

                    stickyBottom?.let { stickyBottomContent ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(screenPaddings(padding))
                                .zIndex(Z_STICKY),
                            contentAlignment = Alignment.Center
                        ) {
                            stickyBottomContent()
                        }
                    }
                }

                if (loadingType == LoadingType.NORMAL) LoadingIndicator()
            }
        }
    }

    BackHandler(enabled = (contentErrorConfig != null) || (onBack != null)) {
        contentErrorConfig?.let {
            contentErrorConfig.onCancel()
        } ?: onBack?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultToolBar(
    navigatableAction: ScreenNavigateAction,
    onBack: (() -> Unit)?,
    keyboardController: SoftwareKeyboardController?,
    toolbarConfig: ToolbarConfig?,
) {

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        title = {
            Text(
                text = toolbarConfig?.title.orEmpty(),
                color = MaterialTheme.colorScheme.primary
            )
        },
        navigationIcon = {
            // Check if we should add back/close button.
            if (navigatableAction != ScreenNavigateAction.NONE) {
                val navigationIcon = when (navigatableAction) {
                    ScreenNavigateAction.CANCELABLE -> AppIcons.Close
                    else -> AppIcons.ArrowBack
                }

                WrapIconButton(
                    iconData = navigationIcon,
                    onClick = {
                        onBack?.invoke()
                        keyboardController?.hide()
                    },
                    customTint = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            ToolBarActions(toolBarActions = toolbarConfig?.actions)
        }
    )
}

@Composable
internal fun ToolBarActions(toolBarActions: List<ToolbarAction>?) {
    toolBarActions?.let { actions ->

        var dropDownMenuExpanded by remember {
            mutableStateOf(false)
        }

        // Show first [MAX_TOOLBAR_ACTIONS] actions.
        actions
            .sortedByDescending { it.order }
            .take(MAX_TOOLBAR_ACTIONS)
            .map { visibleToolbarAction ->
                WrapIconButton(
                    iconData = visibleToolbarAction.icon,
                    onClick = visibleToolbarAction.onClick,
                    enabled = visibleToolbarAction.enabled,
                    customTint = MaterialTheme.colorScheme.primary
                )
            }

        // Check if there are more actions to show.
        if (actions.size > MAX_TOOLBAR_ACTIONS) {
            Box {
                val iconMore = AppIcons.VerticalMore
                WrapIconButton(
                    onClick = { dropDownMenuExpanded = !dropDownMenuExpanded },
                    iconData = iconMore,
                    enabled = true,
                    customTint = MaterialTheme.colorScheme.primary
                )
                DropdownMenu(
                    expanded = dropDownMenuExpanded,
                    onDismissRequest = { dropDownMenuExpanded = false }
                ) {
                    actions
                        .sortedByDescending { it.order }
                        .drop(MAX_TOOLBAR_ACTIONS)
                        .map { dropDownMenuToolbarAction ->
                            val dropDownMenuToolbarActionIcon =
                                dropDownMenuToolbarAction.icon
                            DropdownMenuItem(
                                onClick = dropDownMenuToolbarAction.onClick,
                                enabled = dropDownMenuToolbarAction.enabled,
                                text = {
                                    Text(text = stringResource(id = dropDownMenuToolbarActionIcon.contentDescriptionId))
                                },
                                trailingIcon = {
                                    WrapIcon(
                                        iconData = dropDownMenuToolbarActionIcon,
                                        customTint = MaterialTheme.colorScheme.primary,
                                        contentAlpha = if (dropDownMenuToolbarAction.enabled) {
                                            ALPHA_ENABLED
                                        } else {
                                            ALPHA_DISABLED
                                        }
                                    )
                                }
                            )
                        }
                }
            }
        }
    }
}