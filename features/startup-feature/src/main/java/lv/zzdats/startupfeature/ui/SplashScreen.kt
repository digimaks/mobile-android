// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.startupfeature.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import lv.zzdats.uilogic.components.AppIcons
import lv.zzdats.uilogic.components.utils.OneTimeLaunchedEffect
import lv.zzdats.uilogic.components.wrap.WrapImage
import lv.zzdats.uilogic.navigation.ModuleRoute
import lv.zzdats.uilogic.navigation.StartupScreens

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel
) {
    Content(
        state = viewModel.viewState.value,
        effectFlow = viewModel.effect,
        onNavigationRequested = {
            when (it) {
                is Effect.Navigation.SwitchModule -> {
                    navController.navigate(it.moduleRoute.route) {
                        popUpTo(ModuleRoute.StartupModule.route) { inclusive = true }
                    }
                }

                is Effect.Navigation.SwitchScreen -> {
                    navController.navigate(it.route) {
                        popUpTo(StartupScreens.Splash.screenRoute) { inclusive = true }
                    }
                }
            }
        }
    )

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Initialize)
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit
) {
    val visibilityState = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }
    Scaffold { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visibleState = visibilityState,
                enter = fadeIn(animationSpec = tween(state.logoAnimationDuration)),
                exit = fadeOut(animationSpec = tween(state.logoAnimationDuration)),
            ) {
//                WrapImage(
//                    iconData = AppIcons.Logo
//                )
            }
        }
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)
            }
        }.collect()
    }
}