package com.forgemind.android.ui.audio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
fun rememberAudioPicker(
    onAudioSelected: (Uri?) -> Unit
): () -> Unit {

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            onAudioSelected(uri)
        }

    return {
        launcher.launch("audio/*")
    }
}