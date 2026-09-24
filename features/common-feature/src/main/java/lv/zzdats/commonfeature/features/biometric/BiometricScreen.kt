// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.biometric

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import lv.zzdats.resourceslogic.R
import lv.zzdats.uilogic.components.AppIcons
import lv.zzdats.uilogic.components.content.*
import lv.zzdats.uilogic.components.utils.*
import lv.zzdats.uilogic.components.wrap.*
import lv.zzdats.uilogic.config.FlowCompletion
import lv.zzdats.uilogic.extension.*
import lv.zzdats.uilogic.navigation.CommonScreens
import lv.zzdats.uilogic.navigation.handleDeepLinkAction

@Composable
fun BiometricScreen(
    navController: NavController,
    viewModel: BiometricViewModel
) {
    val state = viewModel.viewState.value
    val context = LocalContext.current

    ContentScreen(
        loadingType = state.isLoading,
        navigatableAction = if (state.isCancellable) {
            ScreenNavigateAction.CANCELABLE
        } else {
            ScreenNavigateAction.NONE
        },
        onBack = { viewModel.setEvent(Event.OnNavigateBack) },
        contentErrorConfig = state.error
    ) {
        Body(
            state = state,
            effectFlow = viewModel.effect,
            onEventSent = { event -> viewModel.setEvent(event) },
            onNavigationRequested = { navigationEffect ->
                handleNavigation(navigationEffect, navController, context, viewModel)
            },
            padding = it
        )
    }

    Event.OnBiometricsClicked(
        context = context,
        shouldThrowErrorIfNotAvailable = true
    )

    OneTimeLaunchedEffect {
        viewModel.setEvent(Event.Init)
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun Body(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSent: (Event) -> Unit,
    onNavigationRequested: (Effect.Navigation) -> Unit,
    padding: PaddingValues
) {
    val context = LocalContext.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                WrapImage(
                    iconData = AppIcons.AppLogo,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f)
                )

                VSpacer.ExtraExtraLarge()

                WrapPrimaryButton(
                    onClick = {
                        onEventSent(
                            Event.OnBiometricsClicked(
                                context = context,
                                shouldThrowErrorIfNotAvailable = true
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.biometric_login_btn_title),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)
                is Effect.InitializeBiometricAuthOnCreate -> {
                    onEventSent(Event.OnBiometricsClicked(context, false))
                }
            }
        }.collect()
    }
}

private fun handleNavigation(
    navigationEffect: Effect.Navigation,
    navController: NavController,
    context: Context,
    viewModel: BiometricViewModel
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screen) {
                popUpTo(CommonScreens.Biometric.screenRoute) {
                    inclusive = true
                }
            }
        }

        is Effect.Navigation.LaunchBiometricsSystemScreen -> {
            viewModel.setEvent(Event.LaunchBiometricSystemScreen)
        }

        is Effect.Navigation.PopBackStackUpTo -> {
            when (navigationEffect.indicateFlowCompletion) {
                FlowCompletion.CANCEL -> {
                    navController.setBackStackFlowCancelled(navigationEffect.screenRoute)
                }

                FlowCompletion.SUCCESS -> {
                    navController.setBackStackFlowSuccess(navigationEffect.screenRoute)
                }

                FlowCompletion.NONE -> {
                    navController.resetBackStack(navigationEffect.screenRoute)
                }
            }
            navController.popBackStack(
                route = navigationEffect.screenRoute,
                inclusive = navigationEffect.inclusive
            )
        }

        is Effect.Navigation.Deeplink -> {
            navigationEffect.routeToPop?.let { route ->
                context.cacheDeepLink(navigationEffect.link)
                if (navigationEffect.isPreAuthorization) {
                    navController.navigate(route) {
                        popUpTo(CommonScreens.Biometric.screenRoute) {
                            inclusive = true
                        }
                    }
                } else {
                    navController.popBackStack(route = route, inclusive = false)
                }
            } ?: handleDeepLinkAction(navController, navigationEffect.link)
        }

        is Effect.Navigation.Pop -> navController.popBackStack()
        is Effect.Navigation.Finish -> context.finish()
    }
}