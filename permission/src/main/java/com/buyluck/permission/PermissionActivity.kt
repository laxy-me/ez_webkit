package com.buyluck.permission

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.Nullable

/**
 * @author yangguangda
 * @date 2018/11/19
 */
class PermissionActivity : Activity() {
    companion object {
        const val KEY_PERMISSIONS = "permissions"
        private const val RC_REQUEST_PERMISSION = 100
        private var CALLBACK: PermissionCallback? = null

        /**
         * 添加一个静态方法方便使用
         */
        @JvmStatic
        fun request(context: Context, permissions: Array<String>, callback: PermissionCallback) {
            CALLBACK = callback
            val intent = Intent(context, PermissionActivity::class.java)
            intent.putExtra(KEY_PERMISSIONS, permissions)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (!intent.hasExtra(KEY_PERMISSIONS)) {
            finish()
            return
        }
        // 当api大于23时，才进行权限申请
        val permissions = getIntent().getStringArrayExtra(KEY_PERMISSIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasSelfPermissions(permissions)) {
                finish()
                CALLBACK?.onPermissionGranted()
            } else {
                requestPermissions(permissions,
                    RC_REQUEST_PERMISSION
                )
            }
        }
    }

    fun hasSelfPermissions(permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != RC_REQUEST_PERMISSION) {
            return
        }
        // 处理申请结果
        val shouldShowRequestPermissionRationale = BooleanArray(permissions.size)
        for (i in permissions.indices) {
            shouldShowRequestPermissionRationale[i] = shouldShowRequestPermissionRationale(permissions[i])
        }
        this.onRequestPermissionsResult(permissions, grantResults, shouldShowRequestPermissionRationale)
    }
    
    @TargetApi(Build.VERSION_CODES.M)
    internal fun onRequestPermissionsResult(permissions: Array<String>, grantResults: IntArray, shouldShowRequestPermissionRationale: BooleanArray) {
        val length = permissions.size
        var granted = 0
        val rationalList: ArrayList<String> = ArrayList()
        val rejectList: ArrayList<String> = ArrayList()
        for (i in 0 until length) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale[i]) {
                    rationalList.add(permissions[i])
                } else {
                    rejectList.add(permissions[i])
                }
            } else {
                granted++
            }
        }
        if (granted == length) {
            CALLBACK?.onPermissionGranted()
        } else if (!rejectList.isEmpty()) {
            CALLBACK?.onPermissionReject(rejectList.toTypedArray())
        } else {
            CALLBACK?.shouldShowRational(rationalList.toTypedArray())
        }
        finish()
    }
}