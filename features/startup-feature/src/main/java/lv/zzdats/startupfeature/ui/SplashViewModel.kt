// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.startupfeature.ui

import android.os.Build
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lv.zzdats.startupfeature.interactor.SplashInteractor
import lv.zzdats.uilogic.mvi.MviViewModel
import lv.zzdats.uilogic.mvi.ViewEvent
import lv.zzdats.uilogic.mvi.ViewSideEffect
import lv.zzdats.uilogic.mvi.ViewState
import lv.zzdats.uilogic.navigation.ModuleRoute
import org.koin.android.annotation.KoinViewModel


data class State(
    val logoAnimationDuration: Int = 1500
) : ViewState

sealed class Event : ViewEvent {
    data object Initialize : Event()
}

sealed class Effect : ViewSideEffect {

    sealed class Navigation : Effect() {
        data class SwitchModule(val moduleRoute: ModuleRoute) : Navigation()
        data class SwitchScreen(val route: String) : Navigation()
    }
}

@KoinViewModel
class SplashViewModel(
    private val interactor: SplashInteractor
) : MviViewModel<Event, State, Effect>() {
    private var _isInitialized = false
    val isInitialized: Boolean get() = _isInitialized

    override fun setInitialState(): State = State()
    init {
        viewModelScope.launch {
            val screenRoute = interactor.getAfterSplashRoute()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setEffect {
                    Effect.Navigation.SwitchScreen(screenRoute)
                }
                _isInitialized = true
            }
        }
    }

    override fun handleEvents(event: Event) {
        when (event) {
            Event.Initialize -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    viewModelScope.launch {
                        // Pre-Android 12: Show custom splash animation
                        delay(viewState.value.logoAnimationDuration.toLong())
                        val screenRoute = interactor.getAfterSplashRoute()
                        setEffect {
                            Effect.Navigation.SwitchScreen(screenRoute)
                        }
                    }
                }
            }
        }
    }
}