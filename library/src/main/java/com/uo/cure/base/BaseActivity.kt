package com.uo.cure.base

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.uo.cure.R
import com.uo.cure.permission.GPermission
import com.uo.cure.permission.PermissionCallback

abstract class BaseActivity : TranslucentActivity() {
    protected var TAG = ""

    @JvmField
    val REQ_CODE_LOGIN = 808

    @JvmField
    val REQ_CODE_PERMISSION = 8008

    private var mShouldHideInputMethod: Boolean = false
    private var mLastFocusView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TAG = this::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        TAG = this::class.java.simpleName
    }

    @Keep
    protected open fun requestPermission(
        permissions: Array<String>,
        ration: IntArray,
        callback: AfterPermissionGranted
    ) {
        GPermission.with(this).permission(
            permissions
        ).callback(object : PermissionCallback {
            override fun onPermissionGranted() {
                callback.permissionGranted()
            }

            override fun shouldShowRational(permissions: Array<String>) {
                showRationaleDialog(permissions, intArrayOf())
            }

            override fun onPermissionReject(permissions: Array<String>) {
                showRejectDialog(permissions, intArrayOf())
            }
        }).request()
    }

    @Keep
    interface AfterPermissionGranted {
        fun permissionGranted()
    }

    @Keep
    protected open fun showRationaleDialog(permissions: Array<String>, ration: IntArray) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.need_permissions))
            .setMessage(permissionDesc(permissions))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.colorPrimary
                )
            )
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            .setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.textColorAssist
                )
            )
    }

    @Keep
    protected open fun showRejectDialog(permissions: Array<String>, reject: IntArray) {
        //拒绝授权
        Log.i(TAG, "permissionsDenied:$permissions")
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permissions in app settings.
        val sb = permissionDesc(permissions)
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.need_permissions))
            .setMessage(sb)
            .setPositiveButton(getString(R.string.go_to_settings)) { dialog, _ ->
                startActivity(getAppDetailSettingIntent())
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.colorPrimary
                )
            )
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            .setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.textColorAssist
                )
            )
    }

    private fun getAppDetailSettingIntent(): Intent {
        val localIntent = Intent()
        localIntent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        localIntent.data = Uri.fromParts("package", packageName, null)
        return localIntent
    }

    @Keep
    protected fun permissionDesc(permissions: Array<String>): StringBuilder {
        val packageManager = packageManager
        val sb = StringBuilder()
        for (permission in permissions) {
            try {
                val permissionInfo = packageManager.getPermissionInfo(permission, 0)
                sb.append("-${getString(permissionInfo.labelRes)}").append("\n")
            } catch (e: PackageManager.NameNotFoundException) {
                sb.append(permission.replace("android.permission.", "")).append("\n")
            }
        }
        return sb
    }

    @Keep
    @RequiresApi(api = Build.VERSION_CODES.M)
    open fun isIgnoringBatteryOptimizations(): Boolean {
        var isIgnoring = false
        val powerManager = getSystemService(Context.POWER_SERVICE)
        powerManager?.let {
            isIgnoring = (it as PowerManager).isIgnoringBatteryOptimizations(packageName)
        }
        return isIgnoring
    }

    @Keep
    @RequiresApi(api = Build.VERSION_CODES.M)
    open fun requestIgnoreBatteryOptimizations() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}