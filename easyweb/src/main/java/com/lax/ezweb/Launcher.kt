package com.lax.ezweb

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import java.lang.ref.WeakReference
import java.util.*

@Keep
class Launcher private constructor() {

    private var mContext: Context? = null

    private val mIntent: Intent = Intent()

    fun putExtra(key: String, value: ArrayList<out Parcelable>): Launcher {
        mIntent.putParcelableArrayListExtra(key, value)
        return this
    }

    fun putExtra(key: String, parcelable: Parcelable): Launcher {
        mIntent.putExtra(key, parcelable)
        return this
    }

    fun putExtra(key: String, value: Int): Launcher {
        mIntent.putExtra(key, value)
        return this
    }

    fun putExtra(key: String, value: Long): Launcher {
        mIntent.putExtra(key, value)
        return this
    }

    fun putExtra(key: String, value: Double): Launcher {
        mIntent.putExtra(key, value)
        return this
    }

    fun putExtra(key: String, value: String): Launcher {
        mIntent.putExtra(key, value)
        return this
    }

    fun putExtra(key: String, value: Boolean): Launcher {
        mIntent.putExtra(key, value)
        return this
    }

    fun putExtra(key: String, value: Array<String>): Launcher {
        mIntent.putExtra(key, value)
        return this
    }

    fun putExtra(key: String, bundle: Bundle): Launcher {
        mIntent.putExtra(key, bundle)
        return this
    }

    fun setFlags(flag: Int): Launcher {
        mIntent.flags = flag
        return this
    }

    fun addFlags(flag: Int): Launcher {
        mIntent.addFlags(flag)
        return this
    }

    fun execute() {
        if (mContext != null) {
            mContext!!.startActivity(mIntent)
        }
    }

    fun execute(requestCode: Int) {
        if (mContext != null) {
            if (mContext is Activity) {
                val activity = mContext as Activity?
                activity!!.startActivityForResult(mIntent, requestCode)
            }
        }
    }

    fun execute(fragment: Fragment?, requestCode: Int) {
        if (mContext != null && fragment != null) {
            fragment.startActivityForResult(mIntent, requestCode)
        }
    }

    @Keep
    companion object {

        private var sInstance: Launcher? = null

        @JvmStatic
        fun with(fragment: Fragment, clazz: Class<*>): Launcher {
            sInstance = Launcher()
            sInstance!!.mContext = WeakReference<Context>(fragment.getActivity()).get()
            sInstance!!.mIntent.setClass(sInstance!!.mContext!!, clazz)
            return sInstance as Launcher
        }

        @JvmStatic
        fun with(context: Context, clazz: Class<*>): Launcher {
            sInstance = Launcher()
            sInstance!!.mContext = WeakReference(context).get()
            sInstance!!.mIntent.setClass(sInstance!!.mContext!!, clazz)
            return sInstance as Launcher
        }
    }
}
