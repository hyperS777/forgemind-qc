package com.forgemind.android.ui.gallery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
fun rememberGalleryPicker(
    onImageSelected: (Uri?) -> Unit
): () -> Unit {

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        onImageSelected(uri)
    }

    return {
        launcher.launch("image/*")
    }
}