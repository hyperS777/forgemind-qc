package com.forgemind.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.forgemind.android.ui.theme.TextSecondary


@Composable
fun ForgeTopBar(
    workerName: String = "Worker"
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

        IconButton(onClick = {}) {

            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Profile"
            )

        }
    }
}