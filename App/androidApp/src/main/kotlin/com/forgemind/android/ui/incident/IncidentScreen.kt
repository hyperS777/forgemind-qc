package com.forgemind.android.ui.incident

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.forgemind.android.network.AudioFileHelper
import com.forgemind.android.network.ImageFileHelper
import com.forgemind.android.network.RetrofitClient
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.forgemind.android.util.NotificationHelper

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
import com.forgemind.android.model.LatestPayload
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

    var backendUrl by rememberSaveable {
        mutableStateOf("")
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

    var showAnomalyDialog by rememberSaveable { mutableStateOf(false) }
    var hasNotified by rememberSaveable { mutableStateOf(false) }
    var showNotificationIcon by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    var livePayload by remember { mutableStateOf<LatestPayload?>(null) }
    
    LaunchedEffect(Unit) {
        while (true) {
            try {
                livePayload = RetrofitClient.api.latestPayload()
            } catch (e: Exception) {}
            delay(3000)
        }
    }

    val diagnosis by viewModel.diagnosis.collectAsState()

    val loading by viewModel.isLoading.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.util.Log.d("IncidentScreen", "Notification permission denied")
        }
    }

    LaunchedEffect(Unit) {
        backendUrl = RetrofitClient.getBaseUrl(context)
    }

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

    // Trigger an in-app dialog and a system notification when a severe diagnosis arrives
    LaunchedEffect(diagnosis) {
        val d = diagnosis ?: return@LaunchedEffect
        android.util.Log.i("ForgeMind", "Diagnosis received: ${d.fault} severity=${d.severity} confidence=${d.confidence}")
        showDiagnosis = true

        val sev = d.severity
        if (sev.equals("High", ignoreCase = true) && !hasNotified) {
            var permissionGranted = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                permissionGranted = status == PackageManager.PERMISSION_GRANTED
                if (!permissionGranted) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            if (permissionGranted) {
                NotificationHelper.showAnomalyNotification(context, "Anomaly detected: ${d.fault}", d.summary)
            } else {
                val result = snackbarHostState.showSnackbar(
                    message = "Notification permission required to show outside-app alerts.",
                    actionLabel = "Settings"
                )
                if (result == SnackbarResult.ActionPerformed) {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            }

            // also show a quick Toast so it's visible immediately on-device
            try { android.widget.Toast.makeText(context, "Anomaly: ${d.fault}", android.widget.Toast.LENGTH_LONG).show() } catch(_: Exception) {}
            try { snackbarHostState.showSnackbar("Anomaly: ${d.fault}") } catch(_: Exception) {}
            hasNotified = true
            showNotificationIcon = true
            showAnomalyDialog = true
        } else if (sev.equals("Warning", ignoreCase = true)) {
            showNotificationIcon = true
            try { snackbarHostState.showSnackbar("Warning detected: ${d.fault}") } catch(_: Exception) {}
        }
    }

    val dLocal = diagnosis
    if (showAnomalyDialog && dLocal != null) {
        AlertDialog(
            onDismissRequest = { showAnomalyDialog = false },
            confirmButton = {
                TextButton(onClick = { showDiagnosis = true; showAnomalyDialog = false }) {
                    Text("View Details")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAnomalyDialog = false }) { Text("Dismiss") }
            },
            title = { Text("Anomaly Detected: ${dLocal.fault}") },
            text = { Text(dLocal.summary + "\nRecommendation: " + dLocal.recommendation) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(20.dp)
        ) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    text = "Incident",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Machine ID : FAN-01",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Backend: ${RetrofitClient.getBaseUrl(context)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = {
                showAnomalyDialog = true
            }) {
                Box {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    if (showNotificationIcon) {
                        Box(modifier = Modifier
                            .size(8.dp)
                            .offset(x = 12.dp, y = (-4).dp)
                            .background(MaterialTheme.colorScheme.error, shape = CircleShape)
                        )
                    }
                }
            }
        }

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
                
                val t = livePayload?.telemetry
                Text("Temperature : ${t?.temperature ?: "--"}°C")
                Text("Current : ${t?.current ?: "--"} A")
                Text("RPM : ${t?.rpm ?: "--"}")
                Text("Anomaly Score : ${t?.anomalyScore ?: "--"}")

            }

        }

        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Backend Connection",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Enter the PC backend URL on your Wi-Fi network, for example http://192.168.1.20:8000"
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    label = { Text("Backend URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val trimmedUrl = backendUrl.trim()
                        if (trimmedUrl.isNotEmpty()) {
                            RetrofitClient.setBaseUrl(trimmedUrl, context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Backend URL")
                }
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
                    if (backendUrl.isNotBlank()) {
                        RetrofitClient.setBaseUrl(backendUrl.trim(), context)
                    }
                    showDiagnosis = false
                    showAnomalyDialog = false
                    showNotificationIcon = false
                    hasNotified = false
                    val t = livePayload?.telemetry
                    viewModel.analyze(
                        imageFile,
                        audioFile,
                        t?.temperature ?: 45.0,
                        t?.current ?: 0.4,
                        t?.rpm ?: 2400,
                        t?.anomalyScore ?: 0.0
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
                            Text("${diagnosis!!.severity} • ${diagnosis!!.confidence}%")
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
                        text = diagnosis!!.summary
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Recommended Action",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = diagnosis!!.recommendation
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
}
