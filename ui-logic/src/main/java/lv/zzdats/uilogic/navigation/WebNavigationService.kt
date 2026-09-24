// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class WebNavigationService {
    private val _navigation = MutableSharedFlow<NavigationCommand>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val navigation = _navigation.asSharedFlow()

    suspend fun navigate(command: NavigationCommand) {
        if (!_navigation.tryEmit(command)) {
            _navigation.emit(command)
        }
    }
}

sealed class NavigationCommand {
    data class ToNative(val route: String) : NavigationCommand()
    data class ToWeb(
        val path: String,
        val params: String? = null,
        val query: String? = null
    ) : NavigationCommand()
    data class ToExternal(val route: String) : NavigationCommand()
    data object Back : NavigationCommand()
}