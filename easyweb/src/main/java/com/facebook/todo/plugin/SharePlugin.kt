package com.facebook.todo.plugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import com.facebook.todo.R
import com.facebook.todo.WebActivity
import com.facebook.todo.data.model.ShareData
import com.facebook.todo.tools.AppInfo
import com.facebook.todo.tools.ToastUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL

class SharePlugin {
    private var shareData: ShareData? = null
    private var activity = WeakReference<Activity>(null)

    companion object {
        const val SHARE_RESULT_CODE = 11111

        private var ins: SharePlugin? = null

        @JvmStatic
        fun getInstance(): SharePlugin {
            if (ins == null) {
                synchronized(SharePlugin::class.java) {
                    if (ins == null) {
                        ins = SharePlugin()
                    }
                }
            }
            return ins!!
        }

        @JvmStatic
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
            if (requestCode == SHARE_RESULT_CODE) {
                ins?.shareCallBack(
                    ins?.shareData?.domainUrl ?: "",
                    ins?.shareData?.inviteCode ?: "",
                    2
                )
            }
            return requestCode == SHARE_RESULT_CODE
        }
    }

    /**
     * 需要app 配合 调用接口 分享链接之后 调用接口
     * /user/userTask/dailyFaceAndWhats.do
     * inviteCode 邀请码
     * type  1:facebook  2 whatsApp
     */
    private fun shareCallBack(domainUrl: String, inviteCode: String, type: Int) {
        runBlocking {
            val response = withContext(Dispatchers.IO) {
                val url =
                    "${domainUrl}/user/userTask/dailyFaceAndWhats.do?inviteCode=${inviteCode}&type=${type}"
                try {
                    val conn = URL(url).openConnection()
                    conn.connectTimeout = 5000
                    conn.connect()
                    (conn as HttpURLConnection).responseCode
                } catch (e: Exception) {
                    e.printStackTrace()
                    500
                }
            }
            if (response in 200..299) {
                Log.e(FacebookPlugin.TAG, "share success")
                val activity = activity.get()
                if (activity is WebActivity) {
                    activity.getWebView().reload()
                }
            }
        }
    }

    fun shareToWhatsApp(context: Activity, data: ShareData) {
        this.shareData = data
        shareWithPackageName(context, "com.whatsapp", "", data.content)
    }

    fun shareToWhatsApp(fragment: Fragment, data: ShareData) {
        this.shareData = data
        shareWithPackageName(fragment, "com.whatsapp", "", data.content)
    }

    private fun shareWithPackageName(
        activity: Activity,
        packageName: String,
        className: String,
        content: String
    ) {
        if (AppInfo.isAPPInstalled(activity, packageName)) {
            try {
                val vIt = Intent(Intent.ACTION_SEND)
                vIt.type = "text/plain"
                vIt.setPackage(packageName)
                if (!TextUtils.isEmpty(className)) {
                    vIt.setClassName(packageName, className)
                }
                vIt.putExtra(Intent.EXTRA_TEXT, content)
                vIt.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                activity.startActivityForResult(vIt, SHARE_RESULT_CODE)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } else {
            ToastUtil.showToast(R.string.app_not_installed)
        }
    }

    private fun shareWithPackageName(
        fragment: Fragment,
        packageName: String,
        className: String,
        content: String
    ) {
        if (fragment.context == null) {
            return
        }
        if (AppInfo.isAPPInstalled(fragment.context as Context, packageName)) {
            try {
                val vIt = Intent(Intent.ACTION_SEND)
                vIt.type = "text/plain"
                vIt.setPackage(packageName)
                if (!TextUtils.isEmpty(className)) {
                    vIt.setClassName(packageName, className)
                }
                vIt.putExtra(Intent.EXTRA_TEXT, content)
                vIt.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                fragment.startActivityForResult(vIt, SHARE_RESULT_CODE)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } else {
            ToastUtil.showToast(R.string.app_not_installed)
        }
    }
}