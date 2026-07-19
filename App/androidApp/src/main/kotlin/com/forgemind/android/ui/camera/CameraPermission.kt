package com.forgemind.android.ui.camera

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

@Composable
fun rememberCameraPermission(
    onGranted: () -> Unit
): () -> Unit {

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                onGranted()
            }

        }

    return {
        launcher.launch(Manifest.permission.CAMERA)
    }
}