// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.controller

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AppUpdateStateController {
    val recommendedUpdateAvailable: StateFlow<Boolean>
    fun setRecommendedUpdateAvailable(isAvailable: Boolean)
}

class AppUpdateStateControllerImpl : AppUpdateStateController {
    private val _recommendedUpdateAvailable = MutableStateFlow(false)
    override val recommendedUpdateAvailable: StateFlow<Boolean> =
        _recommendedUpdateAvailable.asStateFlow()

    override fun setRecommendedUpdateAvailable(isAvailable: Boolean) {
        _recommendedUpdateAvailable.value = isAvailable
    }
}
