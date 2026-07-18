package com.forgemind.android.ui.audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class AudioRecorder(
    private val context: Context
) {

    private var recorder: MediaRecorder? = null
    private lateinit var outputFile: File

    fun start() {
        android.util.Log.d("ForgeMind", "Recorder START")

        outputFile = File(
            context.cacheDir,
            "recording.m4a"
        )
        @Suppress("DEPRECATION")
        recorder = MediaRecorder().apply {

            setAudioSource(MediaRecorder.AudioSource.MIC)

            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            setOutputFile(outputFile.absolutePath)

            prepare()

            start()
        }
    }

    fun stop(): File {

        recorder?.apply {
            stop()
            release()
        }

        recorder = null
        android.util.Log.d(
            "ForgeMind",
            "Recorder STOP | exists=${outputFile.exists()} size=${outputFile.length()}"
        )

        return outputFile
    }
}