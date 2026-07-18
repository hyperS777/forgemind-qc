package com.forgemind.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NotificationAlertCard(
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    text = "FAN-01 • Possible Bent Blade Detected",
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