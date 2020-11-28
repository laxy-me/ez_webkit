package com.eztd.arm.base.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.Keep

@Keep
class PermissionUtil(private val context: Context) {
    private var callback: PermissionCallback? = null
    private var permissions: Array<String>? = null

    fun permission(permissions: Array<String>): PermissionUtil {
        this.permissions = permissions
        return this
    }

    fun callback(callback: PermissionCallback): PermissionUtil {
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
        fun with(context: Context): PermissionUtil {
            return PermissionUtil(context)
        }
    }
}