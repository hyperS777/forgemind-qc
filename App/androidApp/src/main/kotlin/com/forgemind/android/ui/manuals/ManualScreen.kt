package com.forgemind.android.ui.manuals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.forgemind.android.ui.components.SectionCard

@Composable
fun ManualScreen(
    onAskForgeMind: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = "Maintenance Guide",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Cooling Fan Blade Replacement",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Bent Cooling Fan Blade",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(20.dp))

        AssistChip(
            onClick = {},
            label = {
                Text("Moderate • 15 Minutes")
            }
        )

        Spacer(Modifier.height(24.dp))

        SectionCard(
            title = "Required Tools"
        ) {

            Spacer(Modifier.height(12.dp))

            Text("🔧 Phillips Screwdriver")
            Text("🔧 8 mm Wrench")
            Text("🔧 Replacement Fan Blade")

        }

        Spacer(Modifier.height(16.dp))

        SectionCard(
            title = "Safety Precautions"
        ) {

            Spacer(Modifier.height(12.dp))

            Text("⚠ Disconnect the power supply.")
            Text("⚠ Wait until the fan completely stops.")
            Text("⚠ Wear protective gloves.")
            Text("⚠ Inspect the fan guard before removal.")

        }

        Spacer(Modifier.height(16.dp))
        SectionCard(
            title = "Repair Procedure"
        ) {

            Spacer(Modifier.height(12.dp))

            Text("1. Disconnect the power supply.")
            Spacer(Modifier.height(8.dp))

            Text("2. Remove the front protective grill.")
            Spacer(Modifier.height(8.dp))

            Text("3. Unscrew and remove the damaged fan blade.")
            Spacer(Modifier.height(8.dp))

            Text("4. Install the replacement blade securely.")
            Spacer(Modifier.height(8.dp))

            Text("5. Reassemble the grill and tighten all fasteners.")
            Spacer(Modifier.height(8.dp))

            Text("6. Restore power and perform a test run.")

        }

        Spacer(Modifier.height(16.dp))

        SectionCard(
            title = "Verification Checklist"
        ) {

            Spacer(Modifier.height(12.dp))

            Text("✓ Fan rotates freely")
            Text("✓ No abnormal vibration")
            Text("✓ Temperature within normal range")
            Text("✓ RPM stable")
            Text("✓ No unusual sound")

        }

        Spacer(Modifier.height(16.dp))

        SectionCard(
            title = "Need More Help?"
        ) {

            Spacer(Modifier.height(12.dp))

            Text(
                "If you're unsure about any repair step or encounter an unexpected issue, ForgeMind can provide additional guidance."
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onAskForgeMind,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ask ForgeMind")
            }

        }

        Spacer(Modifier.height(30.dp))


    }

}