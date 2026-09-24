// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.extension

import androidx.navigation.NavController

private const val FLOW_CANCELLATION = "FLOW_CANCELLATION"
private const val FLOW_SUCCESS = "FLOW_SUCCESS"

fun NavController.setBackStackFlowCancelled(screenRouter: String) {
    try {
        getBackStackEntry(screenRouter).savedStateHandle.remove<Boolean>(FLOW_SUCCESS)
        getBackStackEntry(screenRouter).savedStateHandle[FLOW_CANCELLATION] = true
    } catch (_: Exception) {
    }
}

fun NavController.setBackStackFlowSuccess(screenRouter: String) {
    try {
        getBackStackEntry(screenRouter).savedStateHandle.remove<Boolean>(FLOW_CANCELLATION)
        getBackStackEntry(screenRouter).savedStateHandle[FLOW_SUCCESS] = true
    } catch (_: Exception) {
    }
}

fun NavController.resetBackStack(screenRouter: String) {
    try {
        getBackStackEntry(screenRouter).savedStateHandle.remove<Boolean>(FLOW_CANCELLATION)
        getBackStackEntry(screenRouter).savedStateHandle.remove<Boolean>(FLOW_SUCCESS)
    } catch (_: Exception) {
    }
}