package com.eztd.arm.base.permission

import android.content.Context
import androidx.annotation.Keep

class PermissionActivity : BasePermissionActivity() {
    companion object {
        /**
         * 添加一个静态方法方便使用
         */
        @JvmStatic
        fun request(context: Context, permissions: Array<String>, callback: PermissionCallback) {
            requestPermission(context, permissions, callback)
        }
    }

}