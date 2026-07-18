package com.forgemind.android.repository

import com.forgemind.android.model.Diagnosis
import com.forgemind.android.network.MultipartHelper
import com.forgemind.android.network.RetrofitClient
import java.io.File

class DiagnosisRepository {

    suspend fun analyze(
        imageFile: File?,
        audioFile: File?,
        temperature: Double,
        current: Double,
        rpm: Int,
        anomalyScore: Double
    ): Diagnosis {

        return RetrofitClient.api.analyze(

            image = MultipartHelper.imagePart(imageFile),

            audio = MultipartHelper.audioPart(audioFile),

            temperature = MultipartHelper.text(temperature.toString()),

            current = MultipartHelper.text(current.toString()),

            rpm = MultipartHelper.text(rpm.toString()),

            anomalyScore = MultipartHelper.text(anomalyScore.toString())

        )

    }

}