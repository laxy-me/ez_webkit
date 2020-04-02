package com.lax.ezweb.permission

/**
 * @author yangguangda
 * @date 2018/11/19
 */
interface PermissionCallback {
    fun onPermissionGranted()

    fun shouldShowRational(permissions: Array<String>)

    fun onPermissionReject(permissions: Array<String>)
}
