package com.forgemind.android.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.forgemind.android.model.NotificationEvent
import com.forgemind.android.model.AcknowledgeRequest
import com.forgemind.android.network.RetrofitClient
import com.forgemind.android.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import androidx.navigation.NavController
import com.forgemind.android.ui.navigation.NavRoutes
import com.forgemind.android.ui.theme.CriticalRed
import com.forgemind.android.ui.theme.WarningAmber
import com.forgemind.android.ui.theme.SuccessGreen
import com.forgemind.android.ui.theme.InfoBlue

@Composable
fun AlertsScreen(navController: NavController? = null) {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf<List<NotificationEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            try {
                val response = RetrofitClient.api.notifications()
                notifications = response.notifications
            } catch (e: Exception) {
                // Ignore connection errors
            } finally {
                isLoading = false
            }
            delay(3000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Alerts", style = MaterialTheme.typography.headlineMedium)

            if (notifications.isNotEmpty()) {
                IconButton(onClick = {
                    scope.launch {
                        try {
                            RetrofitClient.api.clearNotifications()
                            NotificationHelper.cancelAll(context)
                            notifications = emptyList()
                        } catch (e: Exception) {}
                    }
                }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading && notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recent alerts", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(notifications) { notif ->
                    AlertCard(
                        notif = notif,
                        onClick = {
                            navController?.navigate(NavRoutes.Incident.route)
                        },
                        onDismiss = {
                            scope.launch {
                                try {
                                    RetrofitClient.api.acknowledgeNotification(AcknowledgeRequest(notif.id))
                                    NotificationHelper.cancel(context, NotificationHelper.notificationIdFor(notif.id))
                                    notifications = notifications.filter { it.id != notif.id }
                                } catch (e: Exception) {}
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertCard(notif: NotificationEvent, onClick: () -> Unit, onDismiss: () -> Unit) {
    val badgeColor = when (notif.severity) {
        "High", "Critical" -> CriticalRed
        "Medium", "Warning" -> WarningAmber
        "Low" -> SuccessGreen
        else -> InfoBlue
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = notif.severity.uppercase(),
                            color = badgeColor,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SimpleDateFormat("HH:mm:ss").format(Date((notif.timestamp * 1000).toLong())),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = notif.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notif.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (notif.telemetry != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("T: ${notif.telemetry.temperature}°C", style = MaterialTheme.typography.bodySmall, color = badgeColor)
                    Text("C: ${notif.telemetry.current}A", style = MaterialTheme.typography.bodySmall, color = badgeColor)
                    Text("Score: ${notif.telemetry.anomalyScore}", style = MaterialTheme.typography.bodySmall, color = badgeColor)
                }
            }
        }
    }
}
