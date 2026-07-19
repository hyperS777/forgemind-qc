package com.forgemind.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.forgemind.android.ui.theme.TextSecondary


@Composable
fun ForgeTopBar(
    workerName: String = "Worker",
    hasUnreadNotification: Boolean = false,
    onNotificationClick: () -> Unit = {}
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "ForgeMind",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Good Evening, $workerName",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        IconButton(onClick = onNotificationClick) {
            Box {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications"
                )
                if (hasUnreadNotification) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Red, shape = CircleShape)
                    )
                }
            }
        }

        IconButton(onClick = {}) {

            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile"
            )

        }
    }
}
