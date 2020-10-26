package com.eztd.arm.tools

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import com.eztd.arm.BuildConfig

@Keep
class Preference private constructor(context: Context) {

    var gpsAdid: String
        get() = mPrefs.getString(Key.GPS_ADID, "")!!
        set(gaid) = apply(Key.GPS_ADID, gaid)
    var pushId: String
        get() = mPrefs.getString(Key.PUSH_ID, "")!!
        set(pushId) = apply(Key.PUSH_ID, pushId)
    var fcmToken: String
        get() = mPrefs.getString(Key.FCM_TOKEN, "")!!
        set(fcmToken) = apply(Key.FCM_TOKEN, fcmToken)

    @Keep
    internal interface Key {
        companion object {
            const val GPS_ADID = "gps_adid"
            const val PUSH_ID = "pushid"
            const val FCM_TOKEN = "fcm_token"
        }
    }

    private val mPrefs: SharedPreferences

    private val editor: SharedPreferences.Editor
        get() = mPrefs.edit()

    init {
        mPrefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private fun apply(key: String, value: Boolean) {
        editor.putBoolean(key, value).apply()
    }

    private fun apply(key: String, value: String) {
        editor.putString(key, value).apply()
    }

    private fun commit(key: String, value: String) {
        editor.putString(key, value).commit()
    }

    private fun commit(key: String, value: Boolean) {
        editor.putBoolean(key, value).commit()
    }

    private fun apply(key: String, value: Long) {
        editor.putLong(key, value).apply()
    }

    private fun apply(key: String, value: Int) {
        editor.putInt(key, value).apply()
    }

    private fun apply(key: String, value: Float) {
        editor.putFloat(key, value).apply()
    }

    @Keep
    companion object {
        private const val SHARED_PREFERENCES_NAME = BuildConfig.LIBRARY_PACKAGE_NAME + "_prefs"

        private var sInstance: Preference? = null

        @JvmStatic
        fun get(): Preference {
            if (sInstance != null) {
                return sInstance as Preference
            } else {
                throw RuntimeException()
            }
        }

        @JvmStatic
        fun init(context: Context) {
            if (sInstance == null) {
                synchronized(Preference::class.java) {
                    if (sInstance == null) {
                        sInstance = Preference(context)
                    }
                }
            }
        }
    }
}
