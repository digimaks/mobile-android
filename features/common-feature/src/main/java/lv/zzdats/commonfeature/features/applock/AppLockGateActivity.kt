// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.applock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import lv.zzdats.authlogic.applock.AppLockManager
import lv.zzdats.commonfeature.features.biometric.BiometricScreen
import lv.zzdats.commonfeature.features.biometric.BiometricUiConfig
import lv.zzdats.commonfeature.features.biometric.Effect
import lv.zzdats.commonfeature.features.biometric.OnBackNavigationConfig
import lv.zzdats.resourceslogic.R
import lv.zzdats.resourceslogic.theme.ThemeManager
import lv.zzdats.uilogic.config.ConfigNavigation
import lv.zzdats.uilogic.config.NavigationType
import lv.zzdats.uilogic.serializer.UiSerializer
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class AppLockGateActivity : FragmentActivity() {

    private val uiSerializer: UiSerializer by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configBase64 = uiSerializer.toBase64(
            BiometricUiConfig(
                title = getString(R.string.biometric_login_prompt_title),
                subTitle = getString(R.string.biometric_login_prompt_subtitle),
                quickPinOnlySubTitle = getString(R.string.biometric_login_prompt_quickPinOnlySubTitle),
                isPreAuthorization = false,
                shouldInitializeBiometricAuthOnCreate = false,
                onSuccessNavigation = ConfigNavigation(
                    navigationType = NavigationType.Finish
                ),
                onBackNavigationConfig = OnBackNavigationConfig(
                    onBackNavigation = null,
                    hasToolbarCancelIcon = false
                )
            ),
            BiometricUiConfig.Parser
        ).orEmpty()

        setContent {
            ThemeManager.getInstance().Theme {
                val navController = rememberNavController()
                val viewModel = koinViewModel<lv.zzdats.commonfeature.features.biometric.BiometricViewModel>(
                    parameters = { parametersOf(configBase64) }
                )
                val ctx = LocalContext.current

                LaunchedEffect(viewModel) {
                    viewModel.setEvent(
                        lv.zzdats.commonfeature.features.biometric.Event.OnBiometricsClicked(
                            context = ctx,
                            shouldThrowErrorIfNotAvailable = true
                        )
                    )
                }

                LaunchedEffect(viewModel) {
                    viewModel.effect
                        .onEach { eff ->
                            when (eff) {
                                is Effect.Navigation.Finish -> {
                                    AppLockManager.recordAuthenticatedNow()
                                    finish()
                                }
                                is Effect.Navigation.PopBackStackUpTo -> {
                                    if (eff.indicateFlowCompletion ==
                                        lv.zzdats.uilogic.config.FlowCompletion.SUCCESS
                                    ) {
                                        AppLockManager.recordAuthenticatedNow()
                                        finish()
                                    }
                                }
                                is Effect.Navigation.SwitchScreen -> {
                                    AppLockManager.recordAuthenticatedNow()
                                    finish()
                                }
                                is Effect.Navigation.Pop -> {
                                    AppLockManager.recordAuthenticatedNow()
                                    finish()
                                }
                                else -> Unit
                            }
                        }
                        .collect()
                }

                BiometricScreen(navController, viewModel)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    companion object {
        fun launch(context: Context) {
            context.startActivity(
                Intent(context, AppLockGateActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            )
        }
    }
}