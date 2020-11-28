package com.eztd.arm.tools

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.annotation.Keep
import com.eztd.arm.base.Installation
import com.eztd.arm.util.SecurityUtil

@Keep
object AppInfo {
    /**
     * 获取版本名，例如 1.0.1
     * @return version name
     */
    @JvmStatic
    fun getVersionName(context: Context): String {
        var versionName = ""
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = info.versionName
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return versionName
    }

    /**
     * 对比两个版本的前后顺
     * @param current 当前版本
     * @param target 比较的版本
     * @return < 0 current落后target
     */
    @JvmStatic
    fun versionCompare(current: String, target: String): Int {
        if (TextUtils.isEmpty(current) || TextUtils.isEmpty(current)) {
            return 1
        }
        val vals1 = current.split("\\.".toRegex()).toTypedArray()
        val vals2 = current.split("\\.".toRegex()).toTypedArray()
        var i = 0
        //set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.size && i < vals2.size && vals1[i]
                .equals(vals2[i], ignoreCase = true)
        ) {
            i++
        }
        //compare first non-equal ordinal number
        if (i < vals1.size && i < vals2.size) {
            val diff = Integer.valueOf(vals1[i])
                .compareTo(Integer.valueOf(vals2[i]))
            return Integer.signum(diff)
        }
        //the strings are equal or one string is a substring of the other
        //e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        return Integer.signum(vals1.size - vals2.size)
    }

    /**
     * 获取 manifest 里面的 meta-data
     * @param context
     * @param name
     * @return
     */
    @JvmStatic
    fun getMetaData(context: Context, name: String): String {
        val packageManager = context.packageManager
        var result = ""
        try {
            val info = packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            result = info.metaData.get(name)!!.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } finally {
            return result
        }
    }

    @JvmStatic
    fun getUserAgent(context: Context): String {
        val userAgent: String? = System.getProperty("http.agent")
        val sb = StringBuffer()
        var i = 0
        val length = userAgent!!.length
        while (i < length) {
            val c = userAgent[i]
            if (c <= '\u001f' || c >= '\u007f') {
                sb.append(String.format("\\u%04x", c.toInt()))
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    @JvmStatic
    fun checkPermission(context: Context, permission: String): Boolean {
        var result = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            result = try {
                val rest = context.checkSelfPermission(permission)
                rest == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        } else {
            val pm = context.packageManager
            if (pm.checkPermission(
                    permission,
                    context.packageName
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                result = true
            }
        }
        return result
    }

    /**
     * 获取设备id
     */
    @JvmStatic
    fun getDeviceHardwareId(context: Context): String {
        return try {
            var typePrefix = "DEVICE_ID_"
            var deviceId = getAndroidId(context)
            if (TextUtils.isEmpty(deviceId)) {
                typePrefix = "UUID_"
                deviceId = getUUID(context)
            }
            Log.d("AppInfo", "getDeviceHardwareId: " + (typePrefix + deviceId))
            SecurityUtil.md5Encrypt(typePrefix + deviceId)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Returns the Android hardware device ID that is unique between the device + user and app
     * signing. This key will change if the app is uninstalled or its data is cleared. Device factory
     * reset will also result in a value change.
     *
     * @return The android ID
     */
    @JvmStatic
    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String {
        return Settings.System.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    /**
     *  UUID.randomUUID()
     */
    @JvmStatic
    fun getUUID(context: Context): String {
        return Installation.id(context)
    }

    @JvmStatic
    fun isAPPInstalled(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val list = pm.getInstalledPackages(0)
        for (i in list.indices) {
            if (list[i].packageName == packageName) {
                return true
            }
        }
        return false
    }

    /**
     * 跳转到应用市场app详情界面
     * @param appPkg App的包名
     * @param marketPkg 应用市场包名
     */
    @JvmStatic
    fun launchAppDetail(context: Context, appPkg: String, marketPkg: String?) {
        try {
            if (TextUtils.isEmpty(appPkg)) return
            val uri = Uri.parse("market://details?id=$appPkg")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (!TextUtils.isEmpty(marketPkg)) intent.setPackage(marketPkg)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
