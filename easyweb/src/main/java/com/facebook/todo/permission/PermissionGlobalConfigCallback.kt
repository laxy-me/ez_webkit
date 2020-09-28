package com.facebook.todo.permission

import androidx.annotation.Keep

@Keep
abstract class PermissionGlobalConfigCallback {
    abstract fun shouldShowRational(permissions: Array<String>, ration: IntArray)
    abstract fun onPermissionReject(permissions: Array<String>, reject: IntArray)
}
