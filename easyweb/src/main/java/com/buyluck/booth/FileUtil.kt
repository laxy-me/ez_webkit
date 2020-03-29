package com.buyluck.booth

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 *
 * @author yangguangda
 * @date 2018/11/22
 */
class FileUtil {
    companion object {
        private const val folder = "picture"
        @JvmStatic
        fun saveBitmap(context: Context, bitmap: Bitmap): String {
            val savePath: String = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                Environment.getExternalStorageDirectory().absolutePath
            } else {
                context.applicationContext.externalCacheDir!!.absolutePath
            }
            val filePic: File
            return try {
                filePic = File("$savePath/$folder/${generateFileName()}.jpg")
                if (!filePic.exists()) {
                    filePic.parentFile.mkdirs()
                    filePic.createNewFile()
                }
                val fos = FileOutputStream(filePic)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.flush()
                fos.close()
                filePic.absolutePath
            } catch (e: IOException) {
                e.printStackTrace()
                ""
            }
        }

        private fun generateFileName(): String {
            return UUID.randomUUID().toString()
        }
    }
}