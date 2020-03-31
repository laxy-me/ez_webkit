package com.lax.permission

import android.app.Activity
import android.app.Service
import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut

/**
 * @author yangguangda
 * @date 2018/11/19
 */
@Aspect
open class PermissionAspect {
    @Pointcut("execution(@com.lax.permission.Permission * *(..))")
    fun pointcutOnNeedPermissionMethod() {
    }

    @Around("pointcutOnNeedPermissionMethod()")
    fun aroundJoinPoint(joinPoint: ProceedingJoinPoint) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            joinPoint.proceed()
        } else {
            try {
                // 获取方法注解
                val annotation: Permission? =
                    joinPoint.target.javaClass.getAnnotation(Permission::class.java)
                // 获取注解参数，这里我们有3个参数需要获取
                val permissions = annotation?.permissions ?: arrayOf()
                val rationales = annotation?.rationales ?: intArrayOf()
                val rejects = annotation?.rejects ?: intArrayOf()
                val permissionList = mutableListOf(*permissions)

                // 获取上下文
                val `object` = joinPoint.getThis()
                var context: Context? = null
                when (`object`) {
                    is FragmentActivity -> context = `object`
                    is Activity -> context = `object`
                    is Fragment -> context = `object`.context
                    is Service -> context = `object`
                }

                // 申请权限
                GPermission.with(context!!)
                    .permission(permissions)
                    .callback(object :
                        PermissionCallback {
                        override fun onPermissionGranted() {
                            try {
                                // 权限申请通过，执行原方法
                                joinPoint.proceed()
                            } catch (throwable: Throwable) {
                                throwable.printStackTrace()
                            }

                        }

                        override fun shouldShowRational(permissions: Array<String>) {
                            // 申请被拒绝，但没有勾选“不再提醒”，这里我们让外部自行处理
                            GPermission.globalConfigCallback?.shouldShowRational(
                                permissions,
                                rationales
                            )
                        }

                        override fun onPermissionReject(permissions: Array<String>) {
                            // 申请被拒绝，且勾选“不再提醒”，这里我们让外部自行处理
                            GPermission.globalConfigCallback?.onPermissionReject(
                                permissions,
                                rejects
                            )
                        }
                    }).request()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

