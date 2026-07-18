package com.forgemind.android.ui.app

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.forgemind.android.ui.navigation.ForgeMindBottomBar
import com.forgemind.android.ui.navigation.ForgeMindNavGraph

@Composable
fun ForgeMindApp() {

    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            ForgeMindBottomBar(navController)
        }
    ) { innerPadding ->

        ForgeMindNavGraph(
            navController = navController,
            padding = innerPadding
        )

    }
}