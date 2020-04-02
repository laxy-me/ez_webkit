package com.lax.ezweb.permission

/**
 *
 * @author yangguangda
 * @date 2018/11/19
 */
/**
 * 写一个接口，将申请被拒绝的上述两种情况交给调用者自行处理，框架内不处理
 */
abstract class PermissionGlobalConfigCallback {
    abstract fun shouldShowRational(permissions: Array<String>, ration: IntArray)
    abstract fun onPermissionReject(permissions: Array<String>, reject: IntArray)
}
