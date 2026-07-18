package com.forgemind.android.ui.navigation

import androidx.compose.ui.Modifier
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.forgemind.android.network.RetrofitClient
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.forgemind.android.ui.history.HistoryScreen
import com.forgemind.android.ui.alerts.AlertsScreen
import com.forgemind.android.ui.home.HomeScreen
import com.forgemind.android.ui.manuals.ManualScreen
import com.forgemind.android.ui.profile.ProfileScreen
import com.forgemind.android.ui.incident.IncidentScreen
import com.forgemind.android.ui.chat.ForgeMindScreen
import androidx.compose.foundation.layout.padding
@Composable
fun ForgeMindNavGraph(
    navController: NavHostController,
    padding: PaddingValues,
    hasUnreadNotification: Boolean = false,
    onNotificationClick: () -> Unit = {}
) {

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route,
        modifier = Modifier.padding(padding)
    ) {

        composable(NavRoutes.Home.route) {
            HomeScreen(
                hasUnreadNotification = hasUnreadNotification,
                onNotificationClick = onNotificationClick
            )
        }

        composable(NavRoutes.History.route) {
            HistoryScreen()
        }

        composable(NavRoutes.Alerts.route) {
            AlertsScreen(navController)
        }

        composable(NavRoutes.Manuals.route) {
            ManualScreen(
                onAskForgeMind = {
                    navController.navigate(NavRoutes.ForgeMind.route)
                }
            )
        }

        composable(NavRoutes.Profile.route) {
            ProfileScreen()
        }

        composable(NavRoutes.Incident.route) {
            IncidentScreen(
                onManualClick = {
                    navController.navigate(NavRoutes.Manuals.route)
                }
            )
        }

        composable(NavRoutes.ForgeMind.route) {
            ForgeMindScreen()
        }

    }

}
