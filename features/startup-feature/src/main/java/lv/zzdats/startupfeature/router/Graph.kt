// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.startupfeature.router

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.NavType
import androidx.navigation.navDeepLink
import androidx.navigation.navArgument
import lv.zzdats.startupfeature.BuildConfig
import lv.zzdats.startupfeature.ui.ForceUpdateScreen
import lv.zzdats.uilogic.navigation.ModuleRoute
import lv.zzdats.startupfeature.ui.SplashScreen
import lv.zzdats.uilogic.navigation.StartupScreens
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

fun NavGraphBuilder.featureStartupGraph(navController: NavController) {
    navigation(
        startDestination = StartupScreens.Splash.screenRoute,
        route = ModuleRoute.StartupModule.route
    ) {
        composable(
            route = StartupScreens.Splash.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + StartupScreens.Splash.screenRoute
                }
            )
        ) {
            SplashScreen(navController, koinViewModel())
        }

        composable(
            route = StartupScreens.ForceUpdate.screenRoute,
            arguments = listOf(
                navArgument("storeUrl") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            ForceUpdateScreen(
                storeUrl = it.arguments?.getString("storeUrl").orEmpty(),
                inAppUpdateHelper = getKoin().get()
            )
        }
    }
}
