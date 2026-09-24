// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.router

import android.app.Activity
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import lv.zzdats.commonfeature.BuildConfig
import lv.zzdats.commonfeature.features.biometric.BiometricScreen
import lv.zzdats.commonfeature.features.biometric.BiometricUiConfig
import lv.zzdats.commonfeature.features.qr_scan.QrScanScreen
import lv.zzdats.commonfeature.features.qr_scan.QrScanUiConfig
import lv.zzdats.commonfeature.features.security.SecurityErrorScreen
import lv.zzdats.uilogic.navigation.CommonScreens
import lv.zzdats.uilogic.navigation.ModuleRoute
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


fun NavGraphBuilder.featureCommonGraph(navController: NavController) {
    navigation(
        startDestination = CommonScreens.Biometric.screenRoute,
        route = ModuleRoute.CommonModule.route
    ) {
        composable(
            route = CommonScreens.Biometric.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + CommonScreens.Biometric.screenRoute
                }
            ),
            arguments = listOf(
                navArgument(BiometricUiConfig.serializedKeyName) {
                    type = NavType.StringType
                }
            )
        ) {
            BiometricScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(BiometricUiConfig.serializedKeyName).orEmpty()
                        )
                    }
                )
            )
        }

        composable(
            route = CommonScreens.QrScan.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + CommonScreens.QrScan.screenRoute
                }
            ),
            arguments = listOf(
                navArgument(QrScanUiConfig.serializedKeyName) {
                    type = NavType.StringType
                }
            )
        ) {
            QrScanScreen(
                navController,
                koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(QrScanUiConfig.serializedKeyName).orEmpty()
                        )
                    }
                )
            )
        }

        composable(
            route = CommonScreens.SecurityError.screenRoute,
            arguments = listOf(
                navArgument("reason") {
                    type = NavType.StringType
                }
            )
        ) {
            SecurityErrorScreen(
                reason = it.arguments?.getString("reason") ?: "",
                onExit = { (navController.context as? Activity)?.finish() }
            )
        }
    }
}