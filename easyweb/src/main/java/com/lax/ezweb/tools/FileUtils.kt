package com.lax.ezweb.tools

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

/**
 * @author yangguangda
 * @date 2018/12/25
 */
object FileUtils {

    private val TAG = "FileUtils"

    private var sFile: File? = null

    /**
     * ExternalStorage: Traditionally SD card, or a built-in storage in device that is distinct from
     * the protected internal storage and can be mounted as a filesystem on a computer.
     */
    private var sExternalStorageAvailable = false
    private var sExternalStorageWriteable = false

    val isExteralStorageAvailable: Boolean
        get() {
            updateExternalStorageState()
            return sExternalStorageAvailable
        }

    val isExternalStorageWriteable: Boolean
        get() {
            updateExternalStorageState()
            return sExternalStorageWriteable
        }

    val REQ_CODE_ASK_PERMISSION = 1

    private fun updateExternalStorageState() {
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == state) {
            sExternalStorageAvailable = true
            sExternalStorageWriteable = true
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY == state) {
            sExternalStorageAvailable = true
            sExternalStorageWriteable = false
        } else {
            sExternalStorageAvailable = false
            sExternalStorageWriteable = false
        }
    }

    fun registerExternalStorageWatcher(activity: Activity?, receiver: BroadcastReceiver) {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED)
        filter.addAction(Intent.ACTION_MEDIA_REMOVED)
        activity?.registerReceiver(receiver, filter)
    }

    fun unregisterExternalStorageWatcher(activity: Activity?, receiver: BroadcastReceiver) {
        if (activity != null) {
            try {
                activity.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) { // throw when receiver not register
                e.printStackTrace()
            }

        }
    }

    fun isStoragePermissionGranted(activity: Activity, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            ActivityCompat.requestPermissions(activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), requestCode)
            return false
        }
    }

    /**
     * @param fileName
     * @param type     Environment.DIRECTORY_PICTURES 图库、音频地址等
     * @return
     */
    @JvmOverloads
    fun createFile(fileName: String, type: String = ""): File {
        var rootFile: File? = null
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) { // Ues external storage first
            try {
                if (!TextUtils.isEmpty(type)) {
                    rootFile = Environment.getExternalStoragePublicDirectory(type)
                    if (!rootFile!!.mkdirs()) {
                        Log.d(TAG, "ImageUtil: external picture storage is exist")
                    }
                } else {
                    rootFile = Utils.context.getExternalCacheDir()
                    if (rootFile == null) {
                        rootFile = Environment.getExternalStorageDirectory()
                    }
                }
            } catch (e: Exception) { // In case of folder missing, should not be call
                rootFile = Environment.getExternalStorageDirectory()
                e.printStackTrace()
            } finally {
                if (rootFile != null) {
                    Log.d(TAG, "ImageUtil: external storage is " + rootFile.absolutePath)
                }
            }
        } else {
            rootFile = Utils.context.getExternalCacheDir()
        }
        return createFile(rootFile, fileName)
    }


    private fun createFile(root: File?, fileName: String): File {
        val lastIndexOfSeparator = fileName.lastIndexOf(File.separator)
        if (lastIndexOfSeparator != -1) {
            val subDir = fileName.substring(0, lastIndexOfSeparator)
            val newFileName = fileName.substring(lastIndexOfSeparator + 1, fileName.length)
            val fullDir = File(root, subDir)
            if (!fullDir.mkdirs()) {
                Log.d(TAG, "createFile: directory create failure or directory had created")
            }

            if (fullDir.exists()) {
                sFile = File(fullDir, newFileName)
                return sFile as File
            }
            sFile = File(root, newFileName)
            return sFile as File

        } else {
            sFile = File(root, fileName)
            return sFile as File
        }
    }

    fun deleteFile(): Boolean {
        return if (sFile != null) {
            sFile!!.delete()
        } else false
    }

    fun getImageContentUri(context: Context, imageFile: File): Uri? {
        val filePath = imageFile.absolutePath
        val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID), MediaStore.Images.Media.DATA + "=? ",
                arrayOf(filePath), null)
        return if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
            val baseUri = Uri.parse("content://media/external/images/media")
            Uri.withAppendedPath(baseUri, "" + id)
        } else {
            if (imageFile.exists()) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DATA, filePath)
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            } else {
                null
            }
        }
    }

    fun updateFileFromDatabase(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val paths = arrayOf<String>(Environment.getExternalStorageDirectory().toString())
            MediaScannerConnection.scanFile(context, paths,
                    null, null)
            MediaScannerConnection.scanFile(context, arrayOf(file.getAbsolutePath()), null, object : MediaScannerConnection.OnScanCompletedListener {
                override fun onScanCompleted(path: String, uri: Uri) {}
            })
        } else {
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())))
        }
    }
}

