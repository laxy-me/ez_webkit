package com.buyluck.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * @author yangguangda
 * @date 2018/11/19
 */
class GPermission(private val context: Context) {
    private var callback: PermissionCallback? = null
    private var permissions: Array<String>? = null

    fun permission(permissions: Array<String>): GPermission {
        this.permissions = permissions
        return this
    }

    fun callback(callback: PermissionCallback): GPermission {
        this.callback = callback
        return this
    }

    fun request() {
        if (permissions == null || permissions!!.isEmpty()) {
            return
        }
        PermissionActivity.request(
            context,
            permissions!!,
            callback!!
        )
    }

    companion object {
        @JvmStatic
        fun hasSelfPermissions(activity: Activity, permissions: Array<String>): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (permission in permissions) {
                    if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                        return false
                    }
                }
            }
            return true
        }

        @JvmStatic
        var globalConfigCallback: PermissionGlobalConfigCallback? = null
            private set

        fun init(callback: PermissionGlobalConfigCallback) {
            globalConfigCallback = callback
        }

        @JvmStatic
        fun with(context: Context): GPermission {
            return GPermission(context)
        }
    }
}