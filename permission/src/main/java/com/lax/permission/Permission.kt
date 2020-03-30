package com.lax.permission

import androidx.annotation.Keep

/**
 * @author yangguangda
 * @date 2018/11/19
 */
@Keep
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class Permission(/* Permissions */
    val permissions: Array<String>, /* Rationales */
    val rationales: IntArray = [], /* Rejects */
    val rejects: IntArray = []
)
