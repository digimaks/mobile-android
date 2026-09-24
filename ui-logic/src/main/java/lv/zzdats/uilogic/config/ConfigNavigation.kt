// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.config

import lv.zzdats.uilogic.navigation.Screen

data class ConfigNavigation(
    val navigationType: NavigationType,
    val flags: Int = 0,
    val indicateFlowCompletion: FlowCompletion = FlowCompletion.NONE
)

sealed interface NavigationType {
    data object Pop : NavigationType
    data object Finish : NavigationType
    data class PushScreen(
        val screen: Screen,
        val arguments: Map<String, String> = emptyMap(),
        val popUpToScreen: Screen? = null
    ) : NavigationType

    data class PushRoute(
        val route: String,
        val popUpToRoute: String? = null
    ) : NavigationType

    data class PopTo(val screen: Screen) : NavigationType
    data class Deeplink(val link: String, val routeToPop: String? = null) : NavigationType
}

enum class FlowCompletion {
    CANCEL,
    SUCCESS,
    NONE
}