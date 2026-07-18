package com.forgemind.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.forgemind.android.ui.app.ForgeMindApp
import com.forgemind.android.ui.theme.ForgeMindTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {

            ForgeMindTheme {

                ForgeMindApp()

            }

        }

    }

}