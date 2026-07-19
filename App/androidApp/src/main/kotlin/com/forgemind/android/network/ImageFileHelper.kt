package com.forgemind.android.network

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageFileHelper {

    fun bitmapToFile(
        context: Context,
        bitmap: Bitmap
    ): File {

        val file = File.createTempFile(
            "camera_image",
            ".jpg",
            context.cacheDir
        )

        FileOutputStream(file).use {

            bitmap.compress(
                Bitmap.CompressFormat.JPEG,
                95,
                it
            )

        }

        return file

    }

    fun uriToFile(
        context: Context,
        uri: Uri
    ): File {

        val file = File.createTempFile(
            "gallery_image",
            ".jpg",
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