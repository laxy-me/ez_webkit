package com.lax.ezweb.permission

import androidx.annotation.Keep

/**
 * @author yangguangda
 * @date 2018/11/19
 */
@Keep
interface PermissionCallback {
    fun onPermissionGranted()

    fun shouldShowRational(permissions: Array<String>)

    fun onPermissionReject(permissions: Array<String>)
}
