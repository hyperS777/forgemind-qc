package com.forgemind.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.forgemind.android.ui.theme.CardBackground
import com.forgemind.android.ui.theme.TextSecondary

@Composable
fun RecentUpdateCard(
    machineId: String,
    issue: String,
    time: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = machineId,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = issue,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}