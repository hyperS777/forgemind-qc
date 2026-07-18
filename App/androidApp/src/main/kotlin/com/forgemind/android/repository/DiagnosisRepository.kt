package com.forgemind.android.repository

import com.forgemind.android.model.Diagnosis
import com.forgemind.android.network.MultipartHelper
import com.forgemind.android.network.RetrofitClient
import java.io.File

class DiagnosisRepository {

    suspend fun analyze(
        imageFile: File?,
        audioFile: File?
    ): Diagnosis {

        return RetrofitClient.api.analyze(

            image = MultipartHelper.imagePart(imageFile),

            audio = MultipartHelper.audioPart(audioFile),

            temperature = MultipartHelper.text("47.2"),

            current = MultipartHelper.text("0.41"),

            rpm = MultipartHelper.text("2420"),

            anomalyScore = MultipartHelper.text("0.87")

        )

    }

}