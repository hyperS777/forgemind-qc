package com.forgemind.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.forgemind.android.ui.theme.PrimaryBlue
import com.forgemind.android.ui.theme.SurfaceDark
import com.forgemind.android.ui.theme.TextHint
import com.forgemind.android.ui.theme.TextPrimary
import androidx.compose.material.icons.filled.History
data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun ForgeMindBottomBar(
    navController: NavHostController
) {

    val items = listOf(
        BottomNavItem(
            "Home",
            NavRoutes.Home.route,
            Icons.Default.Home
        ),
        BottomNavItem(
            "History",
            NavRoutes.History.route,
            Icons.Default.Notifications
        ),
        BottomNavItem(
            "Manuals",
            NavRoutes.Manuals.route,
            Icons.Default.Build
        ),
        BottomNavItem(
            "Profile",
            NavRoutes.Profile.route,
            Icons.Default.Person
        )
    )

    val navBackStackEntry =
        navController.currentBackStackEntryAsState()

    val currentRoute =
        navBackStackEntry.value?.destination?.route

    NavigationBar(
        containerColor = SurfaceDark
    ) {

        items.forEach { item ->

            NavigationBarItem(

                selected = currentRoute == item.route,

                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },

                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },

                label = {
                    Text(item.title)
                },

                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = TextPrimary,
                    indicatorColor = SurfaceDark,
                    unselectedIconColor = TextHint,
                    unselectedTextColor = TextHint
                )
            )
        }
    }
}