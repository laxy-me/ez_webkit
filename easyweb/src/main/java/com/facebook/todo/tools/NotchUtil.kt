package com.facebook.todo.tools

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * @author yangguangda
 * @date 2018/8/23
 */
object NotchUtil {

    val miuiVersion: Int
        get() {
            try {
                val clz = Class.forName("android.os.SystemProperties")
                val mtd = clz.getMethod("get", String::class.java)
                var `val` = mtd.invoke(null, "ro.miui.ui.version.name") as String
                `val` = `val`.replace("[vV]".toRegex(), "")
                return Integer.parseInt(`val`)
            } catch (e: Exception) {
                return 0
            }

        }

    val oppoNotchSize: IntArray
        get() {
            val mProperty = SystemProperties["ro.oppo.screen.heteromorphism"]
            return intArrayOf(0, 0)
        }

    /**
     * 是否有凹槽
     */
    val NOTCH_IN_SCREEN_VOIO = 0x00000020
    /**
     * 是否有圆角
     */
    val ROUNDED_IN_SCREEN_VOIO = 0x00000008

    val SYS_EMUI = "sys_emui"
    val SYS_MIUI = "sys_miui"
    val SYS_OPPO = "sys_oppo"
    val SYS_VIVO = "sys_vivo"
    val SYS_FLYME = "sys_flyme"
    val SYS_OTHER = "sys_other"
    private val KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code"
    private val KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name"
    private val KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage"
    private val KEY_EMUI_API_LEVEL = "ro.build.hw_emui_api_level"
    private val KEY_EMUI_VERSION = "ro.build.version.emui"
    private val KEY_OPPO_VERSION = "ro.build.version.opporom"
    private val KEY_VIVO_VERSION = "ro.vivo.os.version"
    private val KEY_EMUI_CONFIG_HW_SYS_VERSION = "ro.confg.hw_systemversion"

    // TODO: 2018/11/6  
    //小米
    //华为
    //OPPO
    //VIVO
    //魅族
    val romType: String
        get() {
            var sysType = ""
            if (TextUtils.isEmpty(sysType)) {
                try {
                    sysType = SYS_OTHER
                    val prop = Properties()
                    prop.load(FileInputStream(File(Environment.getRootDirectory(), "build.prop")))
                    if (prop.getProperty(KEY_MIUI_VERSION_CODE, null) != null
                            || prop.getProperty(KEY_MIUI_VERSION_NAME, null) != null
                            || prop.getProperty(KEY_MIUI_INTERNAL_STORAGE, null) != null) {
                        sysType = SYS_MIUI
                    } else if (prop.getProperty(KEY_EMUI_API_LEVEL, null) != null
                            || prop.getProperty(KEY_EMUI_VERSION, null) != null
                            || prop.getProperty(KEY_EMUI_CONFIG_HW_SYS_VERSION, null) != null) {
                        sysType = SYS_EMUI
                    } else if (prop.getProperty(KEY_OPPO_VERSION, null) != null) {
                        sysType = SYS_OPPO
                    } else if (prop.getProperty(KEY_VIVO_VERSION, null) != null) {
                        sysType = SYS_VIVO
                    } else if (meizuFlymeOSFlag.toLowerCase().contains("flyme")) {
                        sysType = SYS_FLYME
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    return sysType
                }

            }
            return sysType
        }

    val meizuFlymeOSFlag: String
        get() = getSystemProperty(
            "ro.build.display.id",
            ""
        )

    fun setDisplayCutMode(activity: Activity) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            val lp = activity.window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            activity.window.attributes = lp
        }
    }

    //在使用LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES的时候，状态栏会显示为白色，这和主内容区域颜色冲突,
    //所以我们要开启沉浸式布局模式，即真正的全屏模式,以实现状态和主体内容背景一致
    fun openFullScreenModel(mAc: Activity) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            mAc.requestWindowFeature(Window.FEATURE_NO_TITLE)
            val lp = mAc.window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            mAc.window.attributes = lp
            val decorView = mAc.window.decorView
            var systemUiVisibility = decorView.systemUiVisibility
            val flags = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            systemUiVisibility = systemUiVisibility or flags
            mAc.window.decorView.systemUiVisibility = systemUiVisibility
        }
    }

    fun hasNotchInScreen(context: Activity): Boolean {
        var ret = false
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            val decorView = context.window.decorView
            if (decorView != null) {
                val rootWindowInsets = decorView.rootWindowInsets
                if (rootWindowInsets != null) {
                    val displayCutout = rootWindowInsets.displayCutout
                    if (displayCutout != null) {
                        val boundingRects = displayCutout.boundingRects
                        if (boundingRects != null && boundingRects.size > 0) {
                            ret = true
                        }
                    }
                }
            }
        } else {
            ret = hasNotchInHuawei(context) || hasNotchInOppo(
                context
            ) || hasNotchInVivo(context) || hasNotchInMIUI()
        }
        return ret
    }

    fun getStandardNotchSize(context: Activity): IntArray {
        val size = intArrayOf(0, 0)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            val decorView = context.window.decorView
            if (decorView != null) {
                val rootWindowInsets = decorView.rootWindowInsets
                if (rootWindowInsets != null) {
                    val displayCutout = rootWindowInsets.displayCutout
                    if (displayCutout != null) {
                        val boundingRects = displayCutout.boundingRects
                        if (boundingRects.size > 0) {
                            val rect = boundingRects[0]
                            size[0] = rect.right - rect.left
                            size[1] = rect.bottom - rect.top
                        }
                    }
                }
            }
        }
        return size
    }

    fun getNotchSize(context: Activity): IntArray {
        val romType = romType
        return if (SYS_EMUI == romType) {
            getHuaweiNotchSize(context)
        } else if (SYS_MIUI == romType) {
            getMIUINotchSize(context)
        } else if (SYS_OPPO == romType) {
            oppoNotchSize
        } else if (SYS_VIVO == romType) {
            getStandardNotchSize(context)
        } else {
            getStandardNotchSize(context)
        }
    }

    fun hasNotchInHuawei(context: Context): Boolean {
        var ret = false
        try {
            val cl = context.classLoader
            val HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil")
            val get = HwNotchSizeUtil.getMethod("hasNotchInHuawei")
            ret = get.invoke(HwNotchSizeUtil) as Boolean
        } catch (e: ClassNotFoundException) {
            Log.e("test", "hasNotchInHuawei ClassNotFoundException")
        } catch (e: NoSuchMethodException) {
            Log.e("test", "hasNotchInHuawei NoSuchMethodException")
        } catch (e: Exception) {
            Log.e("test", "hasNotchInHuawei Exception")
        } finally {
            return ret
        }
    }

    fun getHuaweiNotchSize(context: Context): IntArray {
        var ret = intArrayOf(0, 0)
        try {
            val cl = context.classLoader
            val HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil")
            val get = HwNotchSizeUtil.getMethod("getHuaweiNotchSize")
            ret = get.invoke(HwNotchSizeUtil) as IntArray
        } catch (e: ClassNotFoundException) {
            Log.e("test", "getHuaweiNotchSize ClassNotFoundException")
        } catch (e: NoSuchMethodException) {
            Log.e("test", "getHuaweiNotchSize NoSuchMethodException")
        } catch (e: Exception) {
            Log.e("test", "getHuaweiNotchSize Exception")
        } finally {
            return ret
        }
    }

    fun hasNotchInOppo(context: Context): Boolean {
        return context.packageManager.hasSystemFeature("com.oppo.feature.screen.heteromorphism")
    }

    fun hasNotchInMIUI(): Boolean {
        // https://dev.mi.com/console/doc/detail?pId=1293
        val s = SystemProperties["ro.miui.notch"]
        return "1" == s
    }

    fun getMIUINotchSize(context: Activity): IntArray {
        if (miuiVersion >= 10) {
            // https://dev.mi.com/console/doc/detail?pId=1293
            // MIUI 10 新增了获取刘海宽和高的方法，需升级至8.6.26开发版及以上版本。
            val ret = intArrayOf(0, 0)
            val widthId = context.resources.getIdentifier("notch_width", "dimen", "android")
            if (widthId > 0) {
                val width = context.resources.getDimensionPixelSize(widthId)
                ret[0] = width
            }
            val heightId = context.resources.getIdentifier("notch_height", "dimen", "android")
            if (heightId > 0) {
                val height = context.resources.getDimensionPixelSize(heightId)
                ret[1] = height
            }
            return ret
        } else {
            return getStandardNotchSize(context)
        }
    }

    object SystemProperties {
        operator fun get(key: String): String {
            var value = ""
            var cls: Class<*>? = null
            try {
                cls = Class.forName("android.os.SystemProperties")
                val hideMethod = cls!!.getMethod("get",
                        String::class.java)
                val `object` = cls.newInstance()
                value = hideMethod.invoke(`object`, key) as String
            } catch (e: ClassNotFoundException) {
                Log.e("error", "get error() ", e)
            } catch (e: NoSuchMethodException) {
                Log.e("error", "get error() ", e)
            } catch (e: InstantiationException) {
                Log.e("error", "get error() ", e)
            } catch (e: IllegalAccessException) {
                Log.e("error", "get error() ", e)
            } catch (e: IllegalArgumentException) {
                Log.e("error", "get error() ", e)
            } catch (e: InvocationTargetException) {
                Log.e("error", "get error() ", e)
            }

            return value
        }
    }

    fun hasNotchInVivo(context: Context): Boolean {
        var ret = false
        try {
            val cl = context.classLoader
            val FtFeature = cl.loadClass("com.util.FtFeature")
            val get = FtFeature.getMethod("isFeatureSupport", Int::class.javaPrimitiveType!!)
            ret = get.invoke(FtFeature,
                NOTCH_IN_SCREEN_VOIO
            ) as Boolean

        } catch (e: ClassNotFoundException) {
            Log.e("test", "hasNotchInHuawei ClassNotFoundException")
        } catch (e: NoSuchMethodException) {
            Log.e("test", "hasNotchInHuawei NoSuchMethodException")
        } catch (e: Exception) {
            Log.e("test", "hasNotchInHuawei Exception")
        } finally {
            return ret
        }
    }

    private fun getSystemProperty(key: String, defaultValue: String): String {
        try {
            val clz = Class.forName("android.os.SystemProperties")
            val get = clz.getMethod("get", String::class.java, String::class.java)
            return get.invoke(clz, key, defaultValue) as String
        } catch (e: Exception) {
        }

        return defaultValue
    }
}
