package com.forgemind.android.ui.incident

import com.forgemind.android.network.AudioFileHelper
import com.forgemind.android.network.ImageFileHelper
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

import com.forgemind.android.ui.audio.AudioRecorder
import com.forgemind.android.ui.audio.rememberAudioPermission
import com.forgemind.android.ui.audio.rememberAudioPicker
import com.forgemind.android.ui.camera.rememberCameraCapture
import com.forgemind.android.ui.camera.rememberCameraPermission
import com.forgemind.android.ui.components.AnalysisCard
import com.forgemind.android.ui.gallery.rememberGalleryPicker
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.forgemind.android.viewmodel.IncidentViewModel
import java.io.File
@Composable
fun IncidentScreen(
    onManualClick: () -> Unit
) {

    val context = LocalContext.current
    var imageFile by remember {
        mutableStateOf<File?>(null)
    }

    var audioFile by remember {
        mutableStateOf<File?>(null)
    }

    //---------------- IMAGE ----------------//

    var capturedImage by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var galleryImage by remember {
        mutableStateOf<Uri?>(null)
    }

    //---------------- AUDIO ----------------//

    var isRecording by remember {
        mutableStateOf(false)
    }

    var audioReady by remember {
        mutableStateOf(false)
    }

    var uploadedAudio by remember {
        mutableStateOf<Uri?>(null)
    }

    //---------------- ANALYSIS ----------------//

    var isAnalyzing by remember {
        mutableStateOf(false)
    }

    var showDiagnosis by remember {
        mutableStateOf(false)
    }

    var analysisStatus by remember {
        mutableStateOf("Processing input...")
    }



    //---------------- HELPERS ----------------//

    val recorder = remember {
        AudioRecorder(context)
    }

    val openCamera = rememberCameraCapture {

        capturedImage = it

        if (it != null) {
            imageFile = ImageFileHelper.bitmapToFile(
                context,
                it
            )
        }

    }

    val requestCameraPermission =
        rememberCameraPermission {
            openCamera()
        }

    val viewModel: IncidentViewModel = viewModel()

    val diagnosis by viewModel.diagnosis.collectAsState()

    val loading by viewModel.isLoading.collectAsState()

    val openGallery =
        rememberGalleryPicker {

            galleryImage = it

            if (it != null) {

                imageFile =
                    ImageFileHelper.uriToFile(
                        context,
                        it
                    )

            }

        }

    val requestAudioPermission =
        rememberAudioPermission {

            recorder.start()

            isRecording = true
        }

    val openAudioPicker =
        rememberAudioPicker {

            uploadedAudio = it

            if (it != null) {

                audioReady = true

                audioFile = AudioFileHelper.uriToFile(
                    context,
                    it
                )

            }

        }

    val hasImage =
        capturedImage != null || galleryImage != null

    val hasAudio =
        audioReady || uploadedAudio != null

    val canAnalyze =
        hasImage || hasAudio

    //---------------- ANALYSIS FLOW ----------------//

    LaunchedEffect(isAnalyzing) {

        if (!isAnalyzing)
            return@LaunchedEffect

        showDiagnosis = false

        analysisStatus = "Processing input..."
        delay(1000)

        analysisStatus = "Running AI analysis..."
        delay(1200)

        analysisStatus = "Reviewing maintenance information..."
        delay(1200)

        analysisStatus = "Preparing recommendations..."
        delay(1000)

        isAnalyzing = false
        showDiagnosis = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Incident",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Machine ID : FAN-01",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(18.dp)
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    "Telemetry",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(12.dp))

                Text("Temperature : 47.2°C")
                Text("Current : 0.41 A")
                Text("RPM : 2420")
                Text("Anomaly Score : 0.87")

            }

        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Image Evidence",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = requestCameraPermission,
            modifier = Modifier.fillMaxWidth()
        ) {

            Icon(Icons.Default.CameraAlt, null)

            Spacer(Modifier.width(8.dp))

            Text("Capture Image")

        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = openGallery,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Upload Image")

        }

        capturedImage?.let {

            Spacer(Modifier.height(16.dp))

            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

        }

        galleryImage?.let {

            Spacer(Modifier.height(16.dp))

            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Audio Evidence",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {

                if (!isRecording) {

                    requestAudioPermission()

                } else {

                    val recordedFile = recorder.stop()

                    audioFile = recordedFile

                    isRecording = false
                    audioReady = true

                }

            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Icon(Icons.Default.GraphicEq, null)

            Spacer(Modifier.width(8.dp))

            Text(
                if (isRecording)
                    "Stop Recording"
                else
                    "Record Audio"
            )

        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = openAudioPicker,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Upload Audio")

        }

        if (audioReady) {

            Spacer(Modifier.height(12.dp))

            Text(
                text = "✓ Audio ready for analysis",
                color = MaterialTheme.colorScheme.primary
            )

        }

        if (uploadedAudio != null) {

            Spacer(Modifier.height(8.dp))

            Text(
                text = "✓ Audio attached",
                color = MaterialTheme.colorScheme.primary
            )

        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                showDiagnosis = false
                viewModel.analyze(
                    imageFile,
                    audioFile
                )
            },
            enabled = canAnalyze,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Analyze Machine")

        }

        if (loading) {

            Spacer(Modifier.height(20.dp))

            AnalysisCard(
                status = analysisStatus
            )

        }

        if (diagnosis != null) {

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Diagnosis",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(16.dp))

                    AssistChip(
                        onClick = {},
                        label = {
                            Text("High Confidence • 96%")
                        }
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Detected Issue",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(diagnosis!!.fault)

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "The collected evidence indicates abnormal vibration caused by a bent cooling fan blade. Continued operation may reduce cooling efficiency and increase mechanical stress."
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Recommended Action",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Inspect the fan assembly, replace the damaged blade, verify blade balance, and perform a short operational test before returning the unit to service."
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = onManualClick
                    ) {
                        Text("View Maintenance Guide")
                    }

                }

            }

        }

        Spacer(Modifier.height(30.dp))

    }

}
