// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.startupfeature.interactor

import android.net.Uri
import lv.zzdats.authlogic.applock.AppLockManager
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.commonfeature.features.biometric.BiometricUiConfig
import lv.zzdats.commonfeature.features.biometric.OnBackNavigationConfig
import lv.zzdats.commonfeature.features.issuance.IssuanceFlowUiConfig
import lv.zzdats.commonfeature.features.user.onboarding.OnboardingCoordinator
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.controller.AppUpdateStateController
import lv.zzdats.corelogic.security.SecurityInteractor
import lv.zzdats.corelogic.security.SecurityValidation
import lv.zzdats.resourceslogic.R
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.startupfeature.domain.model.AppUpdateStatus
import lv.zzdats.startupfeature.domain.usecase.CheckAppVersionUseCase
import lv.zzdats.uilogic.config.ConfigNavigation
import lv.zzdats.uilogic.config.NavigationType
import lv.zzdats.uilogic.navigation.CommonScreens
import lv.zzdats.uilogic.navigation.StartupScreens
import lv.zzdats.uilogic.navigation.WebScreens
import lv.zzdats.uilogic.navigation.generateComposableArguments
import lv.zzdats.uilogic.navigation.generateComposableNavigationLink
import lv.zzdats.uilogic.serializer.UiSerializer

interface SplashInteractor {
    suspend fun getAfterSplashRoute(): String
}

class SplashInteractorImpl(
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val onboardingCoordinator: OnboardingCoordinator,
    private val securityInteractor: SecurityInteractor,
    private val prefKeys: PrefKeys,
    private val checkAppVersionUseCase: CheckAppVersionUseCase,
    private val appUpdateStateController: AppUpdateStateController
) : SplashInteractor {

    private val hasDocuments: Boolean
        get() = walletCoreDocumentsController.getAllDocuments().isNotEmpty()

    override suspend fun getAfterSplashRoute(): String {
        val updateStatus = runCatching { checkAppVersionUseCase() }
            .getOrElse { AppUpdateStatus.Error }

        when (updateStatus) {
            is AppUpdateStatus.Mandatory -> {
                return generateComposableNavigationLink(
                    screen = StartupScreens.ForceUpdate,
                    arguments = generateComposableArguments(
                        mapOf("storeUrl" to Uri.encode(updateStatus.storeUrl))
                    )
                )
            }

            is AppUpdateStatus.Recommended -> {
                appUpdateStateController.setRecommendedUpdateAvailable(true)
            }

            else -> appUpdateStateController.setRecommendedUpdateAvailable(false)
        }

        when (val validation = securityInteractor.validateDeviceSecurity()) {
            is SecurityValidation.Invalid -> {
                return generateComposableNavigationLink(
                    screen = CommonScreens.SecurityError,
                    arguments = generateComposableArguments(
                        mapOf("reason" to validation.reason.name)
                    )
                )
            }

            SecurityValidation.Valid -> {
                val currentOnboardingScreen = onboardingCoordinator.getCurrentScreen()
                return when {
                    prefKeys.getAppActivated() &&
                        !AppLockManager.needsReauth() &&
                        currentOnboardingScreen.screenRoute != WebScreens.Activation.screenRoute -> {
                        getBiometricsConfig()
                    }

                    else -> currentOnboardingScreen.screenRoute
                }
            }
        }
    }

    private fun getBiometricsConfig(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.Biometric,
            arguments = generateComposableArguments(
                mapOf(
                    BiometricUiConfig.serializedKeyName to uiSerializer.toBase64(
                        BiometricUiConfig(
                            title = resourceProvider.getString(R.string.biometric_login_prompt_title),
                            subTitle = resourceProvider.getString(R.string.biometric_login_prompt_subtitle),
                            quickPinOnlySubTitle = resourceProvider.getString(R.string.biometric_login_prompt_quickPinOnlySubTitle),
                            isPreAuthorization = true,
                            shouldInitializeBiometricAuthOnCreate = true,
                            onSuccessNavigation = ConfigNavigation(
                                navigationType = NavigationType.PushScreen(
                                    screen =
                                    if (hasDocuments) {
                                        WebScreens.Main
                                    } else {
                                        WebScreens.AddPid
                                    },
                                    arguments = if (!hasDocuments) {
                                        mapOf("flowType" to IssuanceFlowUiConfig.NO_DOCUMENT.name)
                                    } else {
                                        emptyMap()
                                    }
                                )
                            ),
                            onBackNavigationConfig = OnBackNavigationConfig(
                                onBackNavigation = ConfigNavigation(
                                    navigationType = NavigationType.Finish
                                ),
                                hasToolbarCancelIcon = false
                            )
                        ),
                        BiometricUiConfig.Parser
                    ).orEmpty()
                )
            )
        )
    }
}
