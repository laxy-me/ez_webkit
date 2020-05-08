package com.lax.ezweb

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep

@Keep
class Preference private constructor(context: Context) {

    var Gaid: String
        get() = mPrefs.getString(Key.GAID, "")!!
        set(gaid) = apply(Key.GAID, gaid)
    var pushId: String
        get() = mPrefs.getString(Key.PUSH_ID, "")!!
        set(pushId) = apply(Key.PUSH_ID, pushId)

    @Keep
    internal interface Key {
        companion object {
            const val GAID = "gaid"
            const val PUSH_ID = "pushid"
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
        private const val SHARED_PREFERENCES_NAME = BuildConfig.FLAVOR + "_prefs"

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
