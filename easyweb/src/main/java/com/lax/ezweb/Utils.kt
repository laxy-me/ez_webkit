package com.lax.ezweb

import android.annotation.SuppressLint
import android.content.Context

/**
 * @author YangGuangda
 * @date 2017/12/4
 */

class Utils private constructor() {

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var mContext: Context? = null

        /**
         * 初始化工具类
         *
         * @param context 上下文
         */
        fun init(context: Context) {
            mContext = context.applicationContext
        }


        /**
         * 获取ApplicationContext
         *
         * @return ApplicationContext
         */
        val context: Context
            get() {
                if (mContext != null) {
                    return mContext as Context
                }
                throw NullPointerException("u should init first")
            }
    }
}
