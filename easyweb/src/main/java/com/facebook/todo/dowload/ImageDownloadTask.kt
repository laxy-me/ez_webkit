package com.facebook.todo.dowload

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Environment
import android.provider.MediaStore
import com.facebook.todo.ContentProvider
import com.facebook.todo.R
import com.facebook.todo.tools.FileUtils
import com.facebook.todo.tools.ToastUtil
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

    private fun save2Album(bitmap: Bitmap, fileName: String): File {
        val file = FileUtils.createFile(fileName, Environment.DIRECTORY_PICTURES)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            ToastUtil.showToast(R.string.save_success)
        } catch (e: Exception) {
            ToastUtil.showToast(R.string.save_fail)
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
