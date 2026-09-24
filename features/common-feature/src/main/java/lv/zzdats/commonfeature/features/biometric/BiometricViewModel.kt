// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.biometric

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import lv.zzdats.authlogic.controller.auth.BiometricsAuthenticate
import lv.zzdats.authlogic.controller.auth.BiometricsAvailability
import lv.zzdats.authlogic.controller.storage.PinStorageController
import lv.zzdats.authlogic.controller.storage.PinValidationResult
import lv.zzdats.commonfeature.features.wallet.DeleteWalletPartialState
import lv.zzdats.commonfeature.features.wallet.WalletInteractor
import lv.zzdats.resourceslogic.R
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.uilogic.components.content.ContentErrorConfig
import lv.zzdats.uilogic.components.content.LoadingType
import lv.zzdats.uilogic.config.ConfigNavigation
import lv.zzdats.uilogic.config.FlowCompletion
import lv.zzdats.uilogic.config.NavigationType
import lv.zzdats.uilogic.mvi.MviViewModel
import lv.zzdats.uilogic.mvi.ViewEvent
import lv.zzdats.uilogic.mvi.ViewSideEffect
import lv.zzdats.uilogic.mvi.ViewState
import lv.zzdats.uilogic.navigation.CommonScreens
import lv.zzdats.uilogic.navigation.WebScreens
import lv.zzdats.uilogic.navigation.generateComposableArguments
import lv.zzdats.uilogic.navigation.generateComposableNavigationLink
import lv.zzdats.uilogic.serializer.UiSerializer
import org.koin.android.annotation.KoinViewModel

sealed class Event : ViewEvent {
    data class OnBiometricsClicked(
        val context: Context,
        val shouldThrowErrorIfNotAvailable: Boolean
    ) : Event()

    data object LaunchBiometricSystemScreen : Event()
    data object OnNavigateBack : Event()
    data object OnErrorDismiss : Event()
    data object Init : Event()
    data class OnQuickPinEntered(val quickPin: String) : Event()
    data object OnBackspace : Event()
    data object OnBackspaceLongPress : Event()
}

data class State(
    val isLoading: LoadingType = LoadingType.NONE,
    val error: ContentErrorConfig? = null,
    val config: BiometricUiConfig,
    val quickPinError: String? = null,
    val quickPin: String = "",
    val userBiometricsAreEnabled: Boolean = true,
    val isCancellable: Boolean = false,
    val notifyOnAuthenticationFailure: Boolean = true,
    val quickPinSize: Int = 6,
    val firstName: String = ""
) : ViewState

sealed class Effect : ViewSideEffect {
    data object InitializeBiometricAuthOnCreate : Effect()
    sealed class Navigation : Effect() {
        data class SwitchScreen(
            val screen: String,
            val screenPopUpTo: String
        ) : Navigation()

        data class PopBackStackUpTo(
            val screenRoute: String,
            val inclusive: Boolean,
            val indicateFlowCompletion: FlowCompletion
        ) : Navigation()

        data object LaunchBiometricsSystemScreen : Navigation()
        data class Deeplink(
            val link: Uri,
            val isPreAuthorization: Boolean,
            val routeToPop: String? = null
        ) : Navigation()

        data object Pop : Navigation()
        data object Finish : Navigation()
    }
}

@KoinViewModel
class BiometricViewModel(
    private val biometricInteractor: BiometricInteractor,
    private val walletInteractor: WalletInteractor,
    private val uiSerializer: UiSerializer,
    private val biometricConfig: String,
    private val resourceProvider: ResourceProvider,
    private val pinStorageController: PinStorageController
) : MviViewModel<Event, State, Effect>() {

    private val biometricUiConfig
        get() = viewState.value.config

    override fun setInitialState(): State {
        val config = uiSerializer.fromBase64(
            biometricConfig,
            BiometricUiConfig::class.java,
            BiometricUiConfig.Parser
        ) ?: throw RuntimeException("BiometricUiConfig:: is Missing or invalid")



        return State(
            config = config,
            userBiometricsAreEnabled = handleGetBiometricAvailability(),
            isCancellable = config.onBackNavigationConfig.isCancellable,
            firstName = walletInteractor.getFirstName()
        )
    }

    private fun handleGetBiometricAvailability(): Boolean {
        var availability: BiometricsAvailability? = null

        biometricInteractor.getBiometricsAvailability { result ->
            availability = result
        }

        val userEnabled = true
        return availability is BiometricsAvailability.CanAuthenticate && userEnabled

    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                if (biometricUiConfig.shouldInitializeBiometricAuthOnCreate && viewState.value.userBiometricsAreEnabled) {
                    setEffect {
                        Effect.InitializeBiometricAuthOnCreate
                    }
                }
            }

            is Event.OnBackspace -> {
                setState {
                    copy(
                        quickPin = quickPin.dropLast(1),
                        quickPinError = null
                    )
                }
            }

            is Event.OnBackspaceLongPress -> {
                setState {
                    copy(
                        quickPin = "",
                        quickPinError = null
                    )
                }
            }

            is Event.OnBiometricsClicked -> {
                setState { copy(error = null) }
                biometricInteractor.getBiometricsAvailability {
                    when (it) {
                        is BiometricsAvailability.CanAuthenticate -> authenticate(
                            event.context
                        )

                        is BiometricsAvailability.NonEnrolled -> {
                            if (!event.shouldThrowErrorIfNotAvailable) {
                                return@getBiometricsAvailability
                            }
                            setEffect {
                                Effect.Navigation.LaunchBiometricsSystemScreen
                            }
                        }

                        is BiometricsAvailability.Failure -> {
                            if (!event.shouldThrowErrorIfNotAvailable) {
                                return@getBiometricsAvailability
                            }
                            setState {
                                copy(
                                    error = ContentErrorConfig(
                                        errorSubTitle = it.errorMessage,
                                        onCancel = { setEvent(Event.OnErrorDismiss) }
                                    )
                                )
                            }
                        }
                    }
                }
            }

            is Event.LaunchBiometricSystemScreen -> {
                setState { copy(error = null) }
                biometricInteractor.launchBiometricSystemScreen()
            }

            is Event.OnNavigateBack -> {
                setState { copy(error = null) }
                biometricUiConfig.onBackNavigationConfig.onBackNavigation?.let {
                    doNavigation(
                        navigation = it,
                        flowSucceeded = false
                    )
                }
            }

            is Event.OnErrorDismiss -> setState {
                copy(error = null)
            }

            is Event.OnQuickPinEntered -> {
                setState {
                    copy(quickPin = event.quickPin, quickPinError = null)
                }
                authorizeWithPin(event.quickPin)
            }
        }
    }

    private fun authorizeWithPin(pin: String) {
        if (pin.length != viewState.value.quickPinSize) return

        viewModelScope.launch {
            when (val result = pinStorageController.validatePinWithSecurityCheck(pin)) {
                is PinValidationResult.Valid -> {
                    authenticationSuccess()
                }
                is PinValidationResult.Invalid -> {
                    val errorMessage = resourceProvider.getString(R.string.quick_pin_invalid_error) +
                            "\n" + resourceProvider.getString(
                        R.string.biometric_warning_attempts_remaining,
                        result.remainingAttempts
                    )
                    setState {
                        copy(
                            quickPin = "",
                            quickPinError = errorMessage
                        )
                    }
                }
                is PinValidationResult.Timeout -> {
                    setState {
                        copy(
                            quickPin = "",
                            quickPinError = getTimeoutMessage(result.remainingMinutes)
                        )
                    }
                }
                PinValidationResult.DeleteWallet -> {
                    walletInteractor.deleteWallet().collect { state ->
                        when (state) {
                            is DeleteWalletPartialState.Success -> {
                                setEffect {
                                    Effect.Navigation.SwitchScreen(
                                        screen = WebScreens.DeactivationSuccess.screenRoute,
                                        screenPopUpTo = ""
                                    )
                                }
                            }
                            is DeleteWalletPartialState.Failure -> {
                                setState {
                                    copy(quickPinError = state.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getTimeoutMessage(minutes: Int): String {
        return if (minutes == 1) {
            resourceProvider.getString(R.string.pin_timeout_message_singular)
        } else {
            resourceProvider.getString(R.string.pin_timeout_message_plural, minutes)
        }
    }

    private fun authenticate(context: Context) {
        biometricInteractor.authenticateWithBiometrics(
            context = context,
            notifyOnAuthenticationFailure = viewState.value.notifyOnAuthenticationFailure
        ) {
            when (it) {
                is BiometricsAuthenticate.Success -> {
                    authenticationSuccess()
                }

                else -> {}
            }
        }
    }

    private fun authenticationSuccess() {
        doNavigation(
            navigation = biometricUiConfig.onSuccessNavigation,
            flowSucceeded = true
        )
    }

    private fun doNavigation(
        navigation: ConfigNavigation,
        screenRoute: String = CommonScreens.Biometric.screenRoute,
        flowSucceeded: Boolean
    ) {
        navigate(navigation, screenRoute, flowSucceeded)
    }

    private fun navigate(
        navigation: ConfigNavigation,
        screenRoute: String = CommonScreens.Biometric.screenRoute,
        flowSucceeded: Boolean
    ) {
        val navigationEffect: Effect.Navigation = when (val nav = navigation.navigationType) {

            is NavigationType.PopTo -> {
                Effect.Navigation.PopBackStackUpTo(
                    screenRoute = nav.screen.screenRoute,
                    inclusive = false,
                    indicateFlowCompletion = when (navigation.indicateFlowCompletion) {
                        FlowCompletion.CANCEL -> if (!flowSucceeded) FlowCompletion.CANCEL else FlowCompletion.NONE
                        FlowCompletion.SUCCESS -> if (flowSucceeded) FlowCompletion.SUCCESS else FlowCompletion.NONE
                        FlowCompletion.NONE -> FlowCompletion.NONE
                    }
                )
            }

            is NavigationType.PushScreen -> {
                Effect.Navigation.SwitchScreen(
                    generateComposableNavigationLink(
                        screen = nav.screen,
                        arguments = generateComposableArguments(nav.arguments)
                    ),
                    screenPopUpTo = screenRoute
                )
            }

            is NavigationType.PushRoute -> {
                Effect.Navigation.SwitchScreen(
                    nav.route,
                    screenPopUpTo = screenRoute
                )
            }

            is NavigationType.Deeplink -> Effect.Navigation.Deeplink(
                nav.link.toUri(),
                viewState.value.config.isPreAuthorization,
                nav.routeToPop
            )

            is NavigationType.Pop -> Effect.Navigation.Pop
            is NavigationType.Finish -> Effect.Navigation.Finish
        }

        setEffect {
            navigationEffect
        }
    }
}
