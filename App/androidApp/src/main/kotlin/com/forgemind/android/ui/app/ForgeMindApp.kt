package com.forgemind.android.ui.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.forgemind.android.model.NotificationEvent
import com.forgemind.android.model.AcknowledgeRequest
import com.forgemind.android.network.RetrofitClient
import com.forgemind.android.ui.navigation.ForgeMindBottomBar
import com.forgemind.android.ui.navigation.ForgeMindNavGraph
import com.forgemind.android.ui.navigation.NavRoutes
import com.forgemind.android.util.NotificationHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ForgeMindApp() {

    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var latestNotification by remember { mutableStateOf<NotificationEvent?>(null) }
    var hasUnreadNotification by rememberSaveable { mutableStateOf(false) }
    var showAnomalyPopup by remember { mutableStateOf(false) }
    var hasNotificationPermission by rememberSaveable { mutableStateOf(false) }
    // A set of already seen notification IDs to avoid double-alerting if we don't acknowledge them
    val seenNotificationIds = remember { mutableSetOf<String>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (!granted) {
            Toast.makeText(context, "Notification permission denied. Enable app notifications to receive alerts.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            hasNotificationPermission = true
        }

        RetrofitClient.initialize(context)
        while (true) {
            try {
                val response = RetrofitClient.api.notifications()
                if (response.notifications.isNotEmpty()) {
                    response.notifications.forEach { notif ->
                        if (!seenNotificationIds.contains(notif.id)) {
                            seenNotificationIds.add(notif.id)

                            // We have a new notification!
                            latestNotification = notif
                            hasUnreadNotification = true

                            if (notif.severity == "High" || notif.severity == "Critical" || notif.severity == "Warning") {
                                showAnomalyPopup = true
                                val message = notif.message
                                if (hasNotificationPermission) {
                                    NotificationHelper.showAnomalyNotification(
                                        context, notif.title, message, notif.id
                                    )
                                } else {
                                    Toast.makeText(context, "${notif.title}: $message", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore connection errors on poll
            }
            delay(3000)
        }
    }

    if (showAnomalyPopup && latestNotification != null) {
        val notif = latestNotification!!
        AlertDialog(
            onDismissRequest = {
                showAnomalyPopup = false
                hasUnreadNotification = false
            },
            confirmButton = {
                TextButton(onClick = {
                    showAnomalyPopup = false
                    hasUnreadNotification = false
                    navController.navigate(NavRoutes.Incident.route)
                }) {
                    Text("Inspect Machine")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAnomalyPopup = false
                    hasUnreadNotification = false
                    NotificationHelper.cancel(context, NotificationHelper.notificationIdFor(notif.id))
                    scope.launch {
                        try {
                            RetrofitClient.api.acknowledgeNotification(AcknowledgeRequest(notif.id))
                        } catch (_: Exception) {
                            // Keep the local dismissal; the Alerts screen can retry later.
                        }
                    }
                }) {
                    Text("Dismiss")
                }
            },
            title = { Text(notif.title) },
            text = {
                Text("${notif.message}\n\n" +
                     "Timestamp: ${SimpleDateFormat("HH:mm:ss").format(Date((notif.timestamp * 1000).toLong()))}\n" +
                     "Severity: ${notif.severity}")
            }
        )
    }

    Scaffold(
        bottomBar = {
            ForgeMindBottomBar(navController)
        }
    ) { innerPadding ->

        ForgeMindNavGraph(
            navController = navController,
            padding = innerPadding,
            hasUnreadNotification = hasUnreadNotification,
            onNotificationClick = {
                if (hasUnreadNotification) {
                    showAnomalyPopup = true
                } else {
                    navController.navigate(NavRoutes.Incident.route)
                }
            }
        )

    }
}
