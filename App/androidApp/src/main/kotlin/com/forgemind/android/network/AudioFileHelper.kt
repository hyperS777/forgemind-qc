package com.forgemind.android.network

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object AudioFileHelper {

    fun uriToFile(
        context: Context,
        uri: Uri
    ): File {

        val file = File.createTempFile(
            "audio",
            ".m4a",
            context.cacheDir
        )

        context.contentResolver.openInputStream(uri)?.use { input ->

            FileOutputStream(file).use {

                input.copyTo(it)

            }

        }

        return file

    }

}