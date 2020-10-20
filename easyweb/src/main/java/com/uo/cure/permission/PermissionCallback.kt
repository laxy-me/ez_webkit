package com.uo.cure.permission

import androidx.annotation.Keep

@Keep
interface PermissionCallback {
    fun onPermissionGranted()

    fun shouldShowRational(permissions: Array<String>)

    fun onPermissionReject(permissions: Array<String>)
}
