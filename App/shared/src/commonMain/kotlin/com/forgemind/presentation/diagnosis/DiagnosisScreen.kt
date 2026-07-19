package com.forgemind.presentation.diagnosis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DiagnosisScreen(viewModel: DiagnosisViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (val s = state) {
            is DiagnosisUiState.WaitingForFindings -> {
                Text("Waiting for machine findings...")
            }
            is DiagnosisUiState.Diagnosing -> {
                CircularProgressIndicator()
                Text("Analyzing findings against manual...")
            }
            is DiagnosisUiState.Ready -> {
                Text("Machine: ${s.findings.machineId}", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Text("Diagnosis: ${s.diagnosis.faultTitle} (${s.diagnosis.confidence} confidence)")
                Text(s.diagnosis.explanation)

                Text("Repair steps:", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                LazyColumn {
                    items(s.diagnosis.repairSteps) { step -> Text("• $step") }
                }

                Text("Safety:", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                LazyColumn {
                    items(s.diagnosis.safety) { item -> Text("⚠ $item") }
                }

                Text("Tools needed: ${s.diagnosis.tools.joinToString(", ")}")

                Button(onClick = { viewModel.markResolved() }) {
                    Text("Mark Resolved")
                }
            }
            is DiagnosisUiState.Resolved -> {
                Text("✅ Resolved: ${s.faultTitle}")
                Text("Dashboard cleared, machine baseline reset.")
            }
            is DiagnosisUiState.Error -> {
                Text("Error: ${s.message}")
            }
            is DiagnosisUiState.Idle -> {
                Text("Idle")
            }
        }
    }
}
