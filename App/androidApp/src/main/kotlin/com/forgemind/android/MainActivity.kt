package com.forgemind.android

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.forgemind.android.network.RetrofitClient
import com.forgemind.android.ui.app.ForgeMindApp
import com.forgemind.android.ui.theme.ForgeMindTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Initialize networking client and log a clear dev marker so we can confirm
        // that the installed APK is the one we edited.
        try {
            RetrofitClient.initialize(this)
            Log.i("ForgeMind", "DEV BUILD STARTUP: Retrofit initialized with baseUrl=${RetrofitClient.getBaseUrl(this)}")
            Toast.makeText(this, "DEV BUILD: UI patched ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date())}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w("ForgeMind", "DEV BUILD startup logging failed", e)
        }

        setContent {

            ForgeMindTheme {

                ForgeMindApp()

            }

        }

    }

}