// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.webfeature.router

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import lv.zzdats.resourceslogic.bridge.ONBOARDING
import lv.zzdats.signfeature.BuildConfig
import lv.zzdats.uilogic.navigation.CommonScreens
import lv.zzdats.uilogic.navigation.ModuleRoute
import lv.zzdats.uilogic.navigation.WebScreens
import lv.zzdats.webfeature.ui.WebScreen
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.featureWebGraph(navController: NavController) {
    navigation(
        startDestination = WebScreens.AddPid.screenRoute,
        route = ModuleRoute.WebModule.route
    ) {
        composable(
            route = WebScreens.Main.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "dashboard"
                }
            )
        ) {
            WebScreen(navController, koinViewModel())
        }

        composable(
            route = WebScreens.Auth.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "onboarding"
                }
            )
        ) {
            WebScreen(navController, koinViewModel())
        }


        composable(
            route = WebScreens.Activation.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "activation"
                }
            )
        ) {
            WebScreen(navController, koinViewModel())
        }

        composable(
            route = WebScreens.AddDocument.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "add-document"
                }
            )
        ) {
            WebScreen(navController, koinViewModel())
        }

        composable(
            route = WebScreens.ActivationSuccess.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "activation/2"
                }
            )
        ) {
            WebScreen(navController, koinViewModel())
        }

        composable(
            route = WebScreens.DeactivationSuccess.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "activation/deactivated"
                }
            )
        ) {
            WebScreen(navController, koinViewModel())
        }

        composable(
            route = WebScreens.AddPid.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = ONBOARDING.SCREENS.AddPid
                }
            )
        ) {
            WebScreen(navController, koinViewModel())
        }

        composable(
            route = WebScreens.WELCOME.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "onboarding/5"
                }
            )
        ) {
            WebScreen(navController, koinViewModel())
        }

        composable(
            route = WebScreens.LOADING.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "loading"
                }
            )
        ) {
            WebScreen(navController, koinViewModel())
        }

        composable(
            route = WebScreens.DocumentOffer.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "document-offer"
                },
            )
        ) {
            WebScreen(navController, koinViewModel())
        }

        composable(
            route = WebScreens.DocumentOfferCode.screenRoute,
            arguments = listOf(
                navArgument("uri") {
                    type = NavType.StringType
                    defaultValue = "document-offer-code"
                }
            )
        ) {
            WebScreen(navController, koinViewModel())
        }
        composable(
            route = WebScreens.PresentationRequest.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "document-presentation"
                },
            )
        ) {
            WebScreen(navController, koinViewModel())
        }
        composable(
            route = WebScreens.PresentationLoading.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "presentation-loading"
                },
            )
        ) {
            WebScreen(navController, koinViewModel())
        }
        composable(
            route = WebScreens.PresentationSuccess.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "presentation-success"
                },
            )
        ) {
            WebScreen(navController, koinViewModel())
        }
        composable(
            route = WebScreens.QrInvalid.screenRoute,
            arguments = listOf(
                navArgument("path") {
                    type = NavType.StringType
                    defaultValue = "qr-invalid"
                },
            )
        ) {
            WebScreen(navController, koinViewModel())
        }
        composable(
            route = WebScreens.SignFileShare.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = BuildConfig.DEEPLINK + WebScreens.SignFileShare.screenRoute
                }
            ),
            arguments = listOf(
                navArgument("filePath") {
                    type = NavType.StringType
                }
            )
        ) {
            WebScreen(navController, koinViewModel())
        }
    }
}
