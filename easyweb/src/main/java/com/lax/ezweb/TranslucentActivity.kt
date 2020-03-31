package com.lax.ezweb

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import com.lax.ezweb.tools.NotchUtil
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 *
 * @author yangguangda
 * @date 2018/11/6
 */
open class TranslucentActivity : Activity() {

    fun translucentStatusBar() {
        //make full transparent statusBar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // Android 4.4(19) ~ 5.0(21)
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Android 4.4(19)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Android 5.0(21)
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
            window.statusBarColor = Color.TRANSPARENT
        }
        if (NotchUtil.hasNotchInScreen(this)) {
            NotchUtil.setDisplayCutMode(this)
        }
    }

    fun hideStatusBar() {
        if (Build.VERSION.SDK_INT < 16) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            val decorView = window.decorView
            // Hide the status bar.
            val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
            decorView.systemUiVisibility = uiOptions
            // Remember that you should never show the action bar if the
            // status bar is hidden, so hide that too if necessary.
            val actionBar = actionBar
            actionBar?.hide()
        }
    }


    private fun setWindowFlag(activity: Activity, bits: Int, on: Boolean) {
        val win = activity.window
        val winParams = win.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }

    /**
     * 增加View的paddingTop,增加的值为状态栏高度
     */
    fun addStatusBarHeightPaddingTop(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            var extraHeight = getStatusBarHeight()
            if (NotchUtil.hasNotchInScreen(this)) {
                val notchSize = NotchUtil.getNotchSize(this)
                val height = notchSize[1]
                if (height > extraHeight) {
                    extraHeight = height
                }
            }
            view.setPadding(view.paddingLeft, view.paddingTop + extraHeight,
                    view.paddingRight, view.paddingBottom)
        }
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    /**
     * android 6.0设置字体颜色
     */
    fun setStatusBarDarkModeForM(dark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val window = window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT

            var systemUiVisibility = window.decorView.systemUiVisibility
            if (dark) {
                systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                systemUiVisibility = systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            window.decorView.systemUiVisibility = systemUiVisibility
        }
    }

    /**
     * 设置Android状态栏的字体颜色，状态栏为亮色的时候字体和图标是黑色，状态栏为暗色的时候字体和图标为白色
     *
     * @param dark 状态栏字体是否为深色
     */
    private fun setStatusBarFontIconDark(dark: Boolean) {
        // 小米 MIUI
        if (betweenMIUI6And9()) {
            try {
                val window = window
                val clazz = getWindow()::class.java
                val layoutParams = Class.forName("android.view.MiuiWindowManager\$LayoutParams")
                val field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE")
                val darkModeFlag = field.getInt(layoutParams)
                val extraFlagField = clazz.getMethod("setExtraFlags", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                if (dark) {    //状态栏亮色且黑色字体
                    extraFlagField.invoke(window, darkModeFlag, darkModeFlag)
                } else {       //清除黑色字体
                    extraFlagField.invoke(window, 0, darkModeFlag)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else if (isFlyme()) {
            // 魅族FlymeUI
            try {
                val window = window
                val lp = window.attributes
                val darkFlag = WindowManager.LayoutParams::class.java.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON")
                val meizuFlags = WindowManager.LayoutParams::class.java.getDeclaredField("meizuFlags")
                darkFlag.isAccessible = true
                meizuFlags.isAccessible = true
                val bit = darkFlag.getInt(null)
                var value = meizuFlags.getInt(lp)
                if (dark) {
                    value = value or bit
                } else {
                    value = value and bit.inv()
                }
                meizuFlags.setInt(lp, value)
                window.attributes = lp
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        /* android6.0+系统
         这个设置和在xml的style文件中用这个<item name="android:windowLightStatusBar">true</item>属性是一样的
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (dark) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }*/
    }

    /**
     * 判断是否为MIUI[6,9)
     */
    fun betweenMIUI6And9(): Boolean {
        try {
            val clz = Class.forName("android.os.SystemProperties")
            val mtd = clz.getMethod("get", String::class.java)
            var `val` = mtd.invoke(null, "ro.miui.ui.version.name") as String
            `val` = `val`.replace("[vV]".toRegex(), "")
            val version = Integer.parseInt(`val`)
            return version >= 6 && version < 9
        } catch (e: Exception) {
            return false
        }

    }

    private fun isMiui(): Boolean {
        return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"))
    }

    protected fun isFlyme(): Boolean {
        try {
            val method = Build::class.java.getMethod("hasSmartBar")  // max4 会抛出java.lang.NoSuchMethodException: hasSmartBar []
            return method != null
        } catch (e: Exception) {
            return "Meizu".equals(Build.BRAND, ignoreCase = true)
            //            return false;
        }

    }

    private fun getSystemProperty(propName: String): String? {
        val line: String
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            input = BufferedReader(InputStreamReader(p.inputStream), 1024)
            line = input.readLine()
            input.close()
        } catch (ex: IOException) {
            return null
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        return line
    }
}