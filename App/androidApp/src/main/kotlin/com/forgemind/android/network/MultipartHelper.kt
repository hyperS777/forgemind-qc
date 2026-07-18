package com.forgemind.android.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

object MultipartHelper {

    fun text(value: String): RequestBody {

        return value.toRequestBody(
            "text/plain".toMediaType()
        )

    }

    fun imagePart(
        file: File?
    ): MultipartBody.Part? {

        if (file == null) return null

        val body = file.asRequestBody(
            "image/*".toMediaType()
        )

        return MultipartBody.Part.createFormData(
            "image",
            file.name,
            body
        )

    }

    fun audioPart(
        file: File?
    ): MultipartBody.Part? {

        if (file == null) return null

        val body = file.asRequestBody(
            "audio/*".toMediaType()
        )

        return MultipartBody.Part.createFormData(
            "audio",
            file.name,
            body
        )

    }

}