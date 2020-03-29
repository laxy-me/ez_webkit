package com.buyluck.permission

/**
 * @author yangguangda
 * @date 2018/11/19
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Permission(/* Permissions */
        val permissions: Array<String>, /* Rationales */
        val rationales: IntArray = [], /* Rejects */
        val rejects: IntArray = [])
