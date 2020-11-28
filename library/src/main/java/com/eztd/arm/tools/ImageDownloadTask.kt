package com.eztd.arm.tools

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.eztd.arm.R
import com.eztd.arm.provider.ContentProvider
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

@SuppressLint("StaticFieldLeak")
class ImageDownloadTask : AsyncTask<String, Void, File?>() {
    /**
     * 主要是完成耗时的操作
     */
    override fun doInBackground(vararg url: String): File? {
        val imgUrl: URL?
        val bitmap: Bitmap?
        try {
            imgUrl = URL(url[0])
            val conn = imgUrl.openConnection()
            val http = conn as HttpURLConnection
            val length = http.contentLength
            conn.connect()
            //获得图像的字符流
            val `is` = conn.getInputStream()
            val bis = BufferedInputStream(`is`, length)
            bitmap = BitmapFactory.decodeStream(bis)
            bis.close()
            `is`.close()
            return save2Album(bitmap, "${System.currentTimeMillis()}.jpg")
        } catch (e: MalformedURLException) {
            println("[getNetWorkBitmap->]MalformedURLException")
            e.printStackTrace()
        } catch (e: IOException) {
            println("[getNetWorkBitmap->]IOException")
            e.printStackTrace()
        }
        return null
    }

    override fun onPostExecute(result: File?) {
        super.onPostExecute(result)
        if (result == null) {
            return
        }
        saveImage2Gallery(result)
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun save2Album(bitmap: Bitmap, fileName: String): File {
        val file = FileUtils.createFile(fileName, Environment.DIRECTORY_PICTURES)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            Toast.makeText(ContentProvider.autoContext, R.string.save_success, Toast.LENGTH_SHORT)
                .show()
        } catch (e: Exception) {
            Toast.makeText(ContentProvider.autoContext, R.string.save_fail, Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        } finally {
            try {
                fos!!.close()
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
        }
        return file
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun save2AlbumQ(bitmap: Bitmap, fileName: String) {
        // 把图片下载到共有媒体集合中，并在相册中显示
        // 创建ContentValues, 并加入信息
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DESCRIPTION, fileName)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "*/*")
        values.put(MediaStore.Images.Media.TITLE, fileName)
        values.put(
            MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/${fileName}"
        )
        // 插入到ContentResolver，并返回Uri
        ContentProvider.autoContext?.let {
            val insertUri = it.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
            if (insertUri != null) {
                try {
                    // 获取OutputStream
                    val outputStream =
                        it.contentResolver.openOutputStream(insertUri)
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        outputStream.flush()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        ContentProvider.autoContext,
                        R.string.save_fail,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun saveImage2Gallery(file: File) {
        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(
                ContentProvider.autoContext!!.contentResolver,
                file.absolutePath, file.name, null
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        // 最后通知图库更新
        val intent = Intent(
            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
            FileUtils.getImageContentUri(ContentProvider.autoContext!!, file)
        )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        ContentProvider.autoContext!!.sendBroadcast(intent)
        if (file.exists()) {
            file.delete()
            FileUtils.updateFileFromDatabase(ContentProvider.autoContext!!, file)
        }
    }
}
