package com.forgemind.android.ui.navigation

sealed class NavRoutes(val route: String) {

    data object Home : NavRoutes("home")

    data object History : NavRoutes("history")

    data object Alerts : NavRoutes("alerts")

    data object Incident : NavRoutes("incident")

    data object Processing : NavRoutes("processing")

    data object Diagnosis : NavRoutes("diagnosis")

    data object Chat : NavRoutes("chat")

    data object Manuals : NavRoutes("manuals")

    data object ForgeMind : NavRoutes("ForgeMind")

    data object Profile : NavRoutes("profile")

    data object Resolution : NavRoutes("resolution")

}
