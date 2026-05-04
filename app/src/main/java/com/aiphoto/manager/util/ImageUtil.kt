package com.aiphoto.manager.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtil {

    fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val imagesDir = File(context.filesDir, "prompt_images")
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val file = File(imagesDir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun deleteImage(path: String): Boolean {
        val file = File(path)
        return file.exists() && file.delete()
    }

    fun getImageFile(path: String): File? {
        val file = File(path)
        return if (file.exists()) file else null
    }
}
