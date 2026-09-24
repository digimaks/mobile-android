// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import lv.zzdats.analyticslogic.controller.AnalyticsController
import lv.zzdats.businesslogic.extensions.firstPart
import lv.zzdats.businesslogic.extensions.toMapOrEmpty

interface RouterHost {
    fun getNavController(): NavHostController
    fun getNavContext(): Context
    fun userIsLoggedInWithDocuments(): Boolean
    fun userIsLoggedInWithNoDocuments(): Boolean
    fun popToDashboardScreen()
    fun popToIssuanceOnboardingScreen()
    fun isScreenOnBackStackOrForeground(screen: Screen): Boolean

    @Composable
    fun StartFlow(builder: NavGraphBuilder.(NavController) -> Unit)
}

class RouterHostImpl(
    private val analyticsController: AnalyticsController
) : RouterHost {

    private lateinit var navController: NavHostController
    private lateinit var context: Context

    override fun getNavController(): NavHostController = navController
    override fun getNavContext(): Context = context

    @Composable
    override fun StartFlow(builder: NavGraphBuilder.(NavController) -> Unit) {
        navController = rememberNavController()
        context = LocalContext.current
        NavHost(
            navController = navController,
            startDestination = ModuleRoute.StartupModule.route
        ) {
            builder(navController)
        }
        navController.addOnDestinationChangedListener { _, destination, args ->
            destination.route?.let { route ->
                analyticsController.logScreen(route.firstPart("?"), args.toMapOrEmpty())
            }
        }
    }

    override fun userIsLoggedInWithDocuments(): Boolean =
        isScreenOnBackStackOrForeground(getDashboardScreen())

    override fun userIsLoggedInWithNoDocuments(): Boolean =
        isScreenOnBackStackOrForeground(getIssuanceScreen())

    override fun isScreenOnBackStackOrForeground(screen: Screen): Boolean {
        val screenRoute = screen.screenRoute
        try {
            if (navController.currentDestination?.route == screenRoute) {
                return true
            }
            navController.getBackStackEntry(screenRoute)
            return true
        } catch (_: Exception) {
            return false
        }
    }

    override fun popToDashboardScreen() {
        navController.popBackStack(
            route = getDashboardScreen().screenRoute,
            inclusive = false
        )
    }

    override fun popToIssuanceOnboardingScreen() {
        navController.popBackStack(
            route = getIssuanceScreen().screenRoute,
            inclusive = false
        )
    }

    private fun getDashboardScreen(): Screen {
        return DashboardScreens.Dashboard
    }

    private fun getIssuanceScreen(): Screen {
        return IssuanceScreens.AddDocument
    }
}