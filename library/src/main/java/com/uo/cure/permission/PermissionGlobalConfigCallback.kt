package com.uo.cure.permission

import androidx.annotation.Keep

@Keep
interface PermissionGlobalConfigCallback {
    fun shouldShowRational(permissions: Array<String>, ration: IntArray)
    fun onPermissionReject(permissions: Array<String>, reject: IntArray)
}
