package com.eztd.arm.base

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.eztd.arm.R
import com.eztd.arm.base.permission.PermissionUtil
import com.eztd.arm.base.permission.PermissionCallback

abstract class BaseActivity : AppCompatActivity() {
    protected var TAG = ""

    val REQ_LOGIN_CODE = 808

    val REQ_PERMISSION_CODE = 8008

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
        PermissionUtil.with(this).permission(
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
}