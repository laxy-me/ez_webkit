package com.buyluck.booth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.webkit.WebSettings
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException


object AppInfo {

    interface Meta {
        companion object {
            const val UMENG_CHANNEL = "UMENG_CHANNEL"
        }
    }

    /**
     * 获取版本名，例如 1.0.1
     * @return version name
     */
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
     * 获取 manifest 里面的 meta-data
     * @param context
     * @param name
     * @return
     */
    fun getMetaData(context: Context, name: String): String {
        val packageManager = context.packageManager
        var result = ""
        try {
            val info = packageManager.getApplicationInfo(context.packageName,
                    PackageManager.GET_META_DATA)
            result = info.metaData.get(name)!!.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } finally {
            return result
        }
    }

    fun getUserAgent(context: Context): String {
        val userAgent: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                WebSettings.getDefaultUserAgent(context)
            } catch (e: Exception) {
                System.getProperty("http.agent")
            }
        } else {
            System.getProperty("http.agent")
        }
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

    fun checkPermission(context: Context, permission: String): Boolean {
        var result = false
        if (Build.VERSION.SDK_INT >= 23) {
            result = try {
                val rest = context.checkSelfPermission(permission)
                rest == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
        } else {
            val pm = context.packageManager
            if (pm.checkPermission(permission, context.packageName) == PackageManager.PERMISSION_GRANTED) {
                result = true
            }
        }
        return result
    }

    /**
     * @param context
     * @return
     */
    @Deprecated("\n" + "      ")
    fun getDeviceInfo(context: Context): String? {
        try {
            val json = org.json.JSONObject()
            val tm = context
                    .getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            var deviceId: String? = null
            if (checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
                deviceId = tm.deviceId
            }
            var mac: String? = null
            var fstream: FileReader? = null
            try {
                fstream = FileReader("/sys/class/net/wlan0/address")
            } catch (e: FileNotFoundException) {
                fstream = FileReader("/sys/class/net/eth0/address")
            }

            var `in`: BufferedReader? = null
            if (fstream != null) {
                try {
                    `in` = BufferedReader(fstream, 1024)
                    mac = `in`.readLine()
                } catch (e: IOException) {
                } finally {
                    if (fstream != null) {
                        try {
                            fstream.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    }
                    if (`in` != null) {
                        try {
                            `in`.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                    }
                }
            }
            json.put("mac", mac)
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = mac
            }
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            }
            json.put("device_id", deviceId)
            return json.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getDeviceHardwareId(context: Context): String {
        try {
            val tm = context
                    .getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            var typePrefix = "DEVICE_ID_"
            var deviceId = tm.deviceId

            if (TextUtils.isEmpty(deviceId)) {
                deviceId = Build.SERIAL
                typePrefix = "SERIAL_"
            }
            if (TextUtils.isEmpty(deviceId)) {
                val wifi = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                val mac = wifi.connectionInfo.macAddress
                deviceId = mac
                typePrefix = "MAC_"
            }
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                typePrefix = "ANDROID_ID_"
            }
            Log.d("AppInfo", "getDeviceHardwareId: " + (typePrefix + deviceId))
            return SecurityUtil.md5Encrypt(typePrefix + deviceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun getAndroidId(context: Context): String {
        return Settings.System.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun getImei(context: Context): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
        val imei = tm.deviceId
        return imei
    }

    fun isAPPInstalled(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val pinfo = pm.getInstalledPackages(0)
        for (i in pinfo.indices) {
            if (pinfo[i].packageName == packageName) {
                return true
            }
        }
        return false
    }
}
