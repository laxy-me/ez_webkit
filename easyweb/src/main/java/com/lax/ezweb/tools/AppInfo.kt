package com.lax.ezweb.tools

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.webkit.WebSettings
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.lang.StringBuilder
import java.util.*


@Keep
object AppInfo {
    @Keep
    interface Meta {
        companion object {
            const val CHANNEL = "CHANNEL"
        }
    }

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
            if (checkPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                )
            ) {
                deviceId = tm.deviceId
            }
            var mac: String? = null
            var fstream: FileReader? = null
            fstream = try {
                FileReader("/sys/class/net/wlan0/address")
            } catch (e: FileNotFoundException) {
                FileReader("/sys/class/net/eth0/address")
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
                deviceId =
                    Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            }
            json.put("device_id", deviceId)
            return json.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 获取设备id
     */
    @JvmStatic
    fun getDeviceHardwareId(context: Context): String {
        try {
            var typePrefix = "DEVICE_ID_"
            var deviceId = getImei(context)
            if (TextUtils.isEmpty(deviceId)) {
                typePrefix = "SERIAL_"
                deviceId = getSerial(context)
            }
            if (TextUtils.isEmpty(deviceId)) {
                typePrefix = "MAC_"
                deviceId = getMac(context)
            }
            if (TextUtils.isEmpty(deviceId)) {
                typePrefix = "ANDROID_ID_"
                deviceId = getAndroidId(context)
            }
            if (TextUtils.isEmpty(deviceId)) {
                typePrefix = "UUID_"
                deviceId = getUUID(context)
            }
            Log.d("AppInfo", "getDeviceHardwareId: " + (typePrefix + deviceId))
            return SecurityUtil.md5Encrypt(typePrefix + deviceId)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * Returns the unique device ID, for example, the IMEI for GSM and the MEID or ESN for CDMA phones.
     * Return null if device ID is not available.
     * imei会变，主要是因为双卡和全网通的问题
     */
    @TargetApi(Build.VERSION_CODES.O)
    @JvmStatic
    fun getImei(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            return ""
        if (!checkPermission(context, Manifest.permission.READ_PHONE_STATE))
            return ""
        return try {
            val tm =
                context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            tm.deviceId
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    @JvmStatic
    fun getSerial(context: Context): String {
        var serial = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return serial
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
                try {
                    serial = Build.getSerial()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            serial = Build.SERIAL
        }
        if (serial.isBlank() || Build.UNKNOWN == serial) {
            serial = ""
        }
        return serial
    }

    /**
     * 在 Android 6.0（API 级别 23）到 Android 9（API 级别 28）中，无法通过第三方 API 使用 Wi-Fi 和蓝牙等本地设备 Mac 地址。
     * WifiInfo.getMacAddress() 方法和 BluetoothAdapter.getDefaultAdapter().getAddress() 方法
     * 都返回 02:00:00:00:00:00。
     */
    @JvmStatic
    @TargetApi(Build.VERSION_CODES.M)
    fun getMac(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ""
        }
        var macAddress = ""
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        val info = wifiManager.connectionInfo
        if (!wifiManager.isWifiEnabled && checkPermission(
                context,
                Manifest.permission.CHANGE_WIFI_STATE
            )
        ) {
            //必须先打开，才能获取到MAC地址
            wifiManager.isWifiEnabled = true
            wifiManager.isWifiEnabled = false
        }
        if (null != info) {
            try {
                macAddress = info.macAddress
            } catch (e: Exception) {
            }
        }
        return macAddress
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
        val pinfo = pm.getInstalledPackages(0)
        for (i in pinfo.indices) {
            if (pinfo[i].packageName == packageName) {
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
