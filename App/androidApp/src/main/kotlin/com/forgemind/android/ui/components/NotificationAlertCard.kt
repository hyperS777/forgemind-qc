package com.forgemind.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.forgemind.android.model.LatestPayload
import com.forgemind.android.network.RetrofitClient
import kotlinx.coroutines.delay

@Composable
fun NotificationAlertCard(
    onClick: () -> Unit,
    threshold: Double = 0.5
) {

    val context = LocalContext.current
    var latest by remember { mutableStateOf<LatestPayload?>(null) }

    // Poll backend locally for latest payload; only show when anomaly score exceeds threshold
    LaunchedEffect(Unit) {
        try {
            RetrofitClient.initialize(context)
        } catch (_: Exception) {}
        while (true) {
            try {
                val p = RetrofitClient.api.latestPayload()
                latest = p
            } catch (_: Exception) {
                // ignore
            }
            delay(3000)
        }
    }

    val score = latest?.anomalyScore ?: 0.0

    if (latest == null || score < threshold) {
        // render nothing when no significant payload
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // hide immediately locally, then run provided handler which will ack + navigate
                latest = null
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFE082)
        )
    ) {

        Row(
            modifier = Modifier.padding(16.dp)
        ) {

            Icon(
                Icons.Default.WarningAmber,
                contentDescription = null,
                tint = Color(0xFFE65100)
            )

            Spacer(Modifier.width(12.dp))

            Column {

                Text(
                    text = "New Machine Alert",
                    color = Color(0xFF222222),
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "${latest?.machineId ?: "Machine"} • Possible anomaly detected",
                    color = Color(0xFF424242)
                )

                Text(
                    text = "Tap to inspect",
                    color = Color(0xFF616161)
                )

            }

        }

    }

}