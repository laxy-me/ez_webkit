package com.eztd.arm.util

import java.lang.reflect.Method
import java.lang.reflect.ReflectPermission

class MethodUtil {
    companion object {
        fun getClassMethods(cls: Class<*>): Array<Method?>? {
            val uniqueMethods: HashMap<String, Method> = HashMap()
            var currentClass: Class<*>? = cls
            while (currentClass != null && currentClass != Any::class.java) {
                addUniqueMethods(uniqueMethods, currentClass.declaredMethods)

                //获取接口中的所有方法
                val interfaces = currentClass.interfaces
                for (anInterface in interfaces) {
                    addUniqueMethods(uniqueMethods, anInterface.methods)
                }
                //获取父类，继续while循环
                currentClass = currentClass.superclass
            }
            val methods: Collection<Method> = uniqueMethods.values
            return methods.toTypedArray()
        }

        private fun addUniqueMethods(
            uniqueMethods: HashMap<String, Method>,
            methods: Array<Method>
        ) {
            for (currentMethod in methods) {
                if (!currentMethod.isBridge) {
                    //获取方法的签名，格式是：返回值类型#方法名称:参数类型列表
                    val signature: String? = getSignature(currentMethod)
                    //检查是否在子类中已经添加过该方法，如果在子类中已经添加过，则表示子类覆盖了该方法，无须再向uniqueMethods集合中添加该方法了
                    if (!uniqueMethods.containsKey(signature)) {
                        if (canControlMemberAccessible()) {
                            try {
                                currentMethod.isAccessible = true
                            } catch (e: Exception) {
                                // Ignored. This is only a final precaution, nothing we can do.
                            }
                        }
                        signature?.let {
                            uniqueMethods[signature] = currentMethod
                        }
                    }
                }
            }
        }

        private fun getSignature(method: Method): String? {
            val sb = StringBuilder()
            val returnType = method.returnType
            sb.append(returnType.name).append('#')
            sb.append(method.name)
            val parameters = method.parameterTypes
            for (i in parameters.indices) {
                if (i == 0) {
                    sb.append(':')
                } else {
                    sb.append(',')
                }
                sb.append(parameters[i].name)
            }
            return sb.toString()
        }

        /**
         * Checks whether can control member accessible.
         *
         * @return If can control member accessible, it return true
         * @since 3.5.0
         */
        private fun canControlMemberAccessible(): Boolean {
            try {
                val securityManager = System.getSecurityManager()
                securityManager?.checkPermission(ReflectPermission("suppressAccessChecks"))
            } catch (e: SecurityException) {
                return false
            }
            return true
        }
    }
}