package com.forgemind.android.ui.camera

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

@Composable
fun rememberCameraCapture(
    onImageCaptured: (Bitmap?) -> Unit
): () -> Unit {

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        onImageCaptured(bitmap)
    }

    return {
        launcher.launch(null)
    }
}