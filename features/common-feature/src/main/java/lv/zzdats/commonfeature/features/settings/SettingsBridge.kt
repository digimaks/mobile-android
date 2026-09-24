// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.settings

import android.annotation.SuppressLint
import android.os.LocaleList
import androidx.biometric.BiometricManager
import kotlinx.coroutines.launch
import lv.zzdats.authlogic.controller.auth.BiometricsAvailability
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationResult
import lv.zzdats.authlogic.model.BiometricCrypto
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.zzdats.commonfeature.features.biometric.BiometricInteractor
import lv.zzdats.commonfeature.features.wallet.DeleteWalletPartialState
import lv.zzdats.commonfeature.features.wallet.WalletInteractor
import lv.zzdats.resourceslogic.bridge.SETTINGS
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.uilogic.navigation.NavigationCommand.ToNative
import lv.zzdats.uilogic.navigation.WebScreens
import lv.zzdats.uilogic.navigation.WebNavigationService
import lv.zzdats.webbridge.core.BaseBridge
import lv.zzdats.webbridge.core.BridgeRequest
import lv.zzdats.webbridge.core.BridgeResponse
import java.util.Locale

class SettingsBridge (
    private val biometricInteractor: BiometricInteractor,
    private val resourceProvider: ResourceProvider,
    private val navigationService: WebNavigationService,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val walletInteractor: WalletInteractor,
    private val prefKeys: PrefKeys
): BaseBridge() {
    override fun getName() = SETTINGS.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when(request.function) {
            SETTINGS.ENABLE_BIOMETRICS -> handleEnableBiometrics(request)
            SETTINGS.GET_BIOMETRIC_AVAILABILITY -> handleGetBiometricAvailability(request)
            SETTINGS.SET_THEME -> handleSetTheme(request)
            SETTINGS.SET_LANGUAGE -> handleSetLanguage(request)
            SETTINGS.DELETE_WALLET -> handleDeleteWallet(request)
            else -> createErrorResponse(request, "Unknown function ${request.function}")
        }
    }

    @SuppressLint("RestrictedApi")
    private fun handleEnableBiometrics(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val enabled = data["enabled"] as? Boolean
            ?: return createErrorResponse(request, "Missing enabled parameter")

        val activity = findHostFragmentActivity()
        if (activity == null) {
            val response = createErrorResponse(request, "activity_missing")
            emitEvent(response)
            return response
        }

        deviceAuthenticationInteractor.authenticateWithBiometrics(
            context = activity,
            crypto = BiometricCrypto(null),
            notifyOnAuthenticationFailure = true,
            resultHandler = DeviceAuthenticationResult(
                onAuthenticationSuccess = {
                    biometricInteractor.storeBiometricsUsageDecision(enabled)
                    emitEvent(createSuccessResponse(request, null))
                },
                onAuthenticationError = {
                    emitEvent(createErrorResponse(request, "authentication_error"))
                }
            )
        )

        return createSuccessResponse(request, null)
    }

    private fun handleGetBiometricAvailability(request: BridgeRequest): BridgeResponse {
        var availability: BiometricsAvailability? = null

        biometricInteractor.getBiometricsAvailability { result ->
            availability = result
        }

        val userEnabled = biometricInteractor.getBiometricUserSelection()

        val biometricManager = BiometricManager.from(resourceProvider.provideContext())
        val hasBiometricHardware = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS

        val type = when {
            availability is BiometricsAvailability.CanAuthenticate &&
                    userEnabled && hasBiometricHardware -> "exists"

            availability is BiometricsAvailability.CanAuthenticate &&
                    hasBiometricHardware -> "android"

            else -> null
        }

        emitEvent(createSuccessResponse(request, mapOf("type" to null)))
        return createSuccessResponse(request, mapOf("type" to null))
    }

    private fun handleSetTheme(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val theme = data["theme"] as? String
            ?: return createErrorResponse(request, "Missing theme")

        return createSuccessResponse(request, null)
    }

    private fun handleSetLanguage(request: BridgeRequest): BridgeResponse {
        val data = request.data as? Map<*, *>
            ?: return createErrorResponse(request, "Invalid request data")

        val language = data["language"] as? String
            ?: return createErrorResponse(request, "Missing language")

        try {
            val locale = Locale(language)
            val context = resourceProvider.provideContext()

            Locale.setDefault(locale)
            val config = context.resources.configuration

            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)

            context.resources.updateConfiguration(config, context.resources.displayMetrics)

            // Persist the language preference
            prefKeys.setLanguage(language)

            emitEvent(createSuccessResponse(request, null))
            return createSuccessResponse(request, null)
        } catch (e: Exception) {
            return createErrorResponse(request, "Invalid language format")
        }
    }

    @SuppressLint("RestrictedApi")
    private fun handleDeleteWallet(request: BridgeRequest): BridgeResponse {
        val activity = findHostFragmentActivity()
        if (activity == null) {
            val response = createErrorResponse(request, "activity_missing")
            emitEvent(response)
            return response
        }

        deviceAuthenticationInteractor.getBiometricsAvailability { availability ->
            when (availability) {
                is BiometricsAvailability.CanAuthenticate -> {
                    deviceAuthenticationInteractor.authenticateWithBiometrics(
                        context = activity,
                        crypto = BiometricCrypto(null),
                        notifyOnAuthenticationFailure = true,
                        resultHandler = DeviceAuthenticationResult(
                            onAuthenticationSuccess = {
                                coroutineScope.launch {
                                    walletInteractor.deleteWallet()
                                        .collect { state ->
                                            when (state) {
                                                is DeleteWalletPartialState.Success -> {
                                                    navigationService.navigate(
                                                        ToNative(WebScreens.DeactivationSuccess.screenRoute)
                                                    )
                                                    emitEvent(createSuccessResponse(request, null))
                                                }
                                                is DeleteWalletPartialState.Failure -> {
                                                    emitEvent(createErrorResponse(request, state.error))
                                                }
                                            }
                                        }
                                }
                            },
                            onAuthenticationError = {
                                emitEvent(createErrorResponse(request, "authentication_error"))
                            }
                        )
                    )
                }
                is BiometricsAvailability.NonEnrolled -> {
                    deviceAuthenticationInteractor.launchBiometricSystemScreen()
                    emitEvent(createErrorResponse(request, "BIOMETRIC_NOT_ENROLLED"))
                }
                is BiometricsAvailability.Failure -> {
                    emitEvent(createErrorResponse(request, availability.errorMessage))
                }
            }
        }

        return createSuccessResponse(request, null)
    }
}
