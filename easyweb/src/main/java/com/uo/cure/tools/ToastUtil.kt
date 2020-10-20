package com.uo.cure.tools


import android.content.Context
import android.text.TextUtils
import android.widget.Toast


class ToastUtil private constructor() {

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }

    companion object {
        private var toast: Toast? = null
        private var sContext: Context? = null

        @JvmStatic
        fun init(context: Context) {
            if (toast == null) {
                synchronized(ToastUtil::class.java) {
                    if (toast == null) {
                        sContext = context
                        toast = Toast.makeText(context, "", Toast.LENGTH_SHORT)
                    }
                }
            }
        }

        @JvmStatic
        fun showToast(text: String) {
            if (!TextUtils.isEmpty(text)) {
                toast!!.setText(text)
                toast!!.show()
            }
        }

        fun showToast(stringId: Int) {
            val text = sContext!!.getString(stringId)
            if (!TextUtils.isEmpty(text)) {
                toast!!.setText(text)
                toast!!.show()
            }
        }
    }
}
