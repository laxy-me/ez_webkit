package com.lax.ezweb

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustEvent
import com.google.gson.Gson
import com.lax.ezweb.model.ShareData
import com.lax.ezweb.tools.AppInfo
import com.lax.ezweb.tools.Utils

class AppJs(private val mContext: Context) {

    /**
     * 获取设备号
     */
    @JavascriptInterface
    fun getDeviceId(): String? {
        var deviceHardwareId = AppInfo.getDeviceHardwareId(mContext)
        if (deviceHardwareId.isEmpty()) {
            deviceHardwareId = Preference.get().Gaid
        }
        Log.v(TAG, "getDeviceId:${deviceHardwareId}")
        return deviceHardwareId
    }

    /**
     * 获取个推设备id
     */
    @JavascriptInterface
    fun takePushId(): String {
        Log.v(TAG, "takePushId:${Preference.get().pushId}")
        return Preference.get().pushId
    }

    /**
     * 获取渠道
     */
    @JavascriptInterface
    fun takeChannel(): String {
        Log.v(TAG, "takeChannel:${AppInfo.getMetaData(mContext, "UMENG_CHANNEL")}")
        return AppInfo.getMetaData(mContext, "UMENG_CHANNEL")
    }

    /**
     * 就是设备id
     */
    @JavascriptInterface
    fun getGoogleId(): String {
        var deviceHardwareId = AppInfo.getDeviceHardwareId(mContext)
        if (deviceHardwareId.isEmpty()) {
            deviceHardwareId = Preference.get().Gaid
        }
        Log.v(TAG, "getGoogleId:${deviceHardwareId}")
        return deviceHardwareId
    }

    /**
     * 谷歌广告id
     */
    @JavascriptInterface
    fun getGaId(): String? {
        Log.v(TAG, "getGaId:${Preference.get().Gaid}")
        return Preference.get().Gaid;
    }

    /**
     * 调取谷歌登录方法
     */
    @JavascriptInterface
    fun openGoogle(data: String) {
        if (mContext is WebActivity) {
            Log.v(TAG, "openGoogle:${data}")
            mContext.googleLogin(data)
        }
    }

    /**
     * adjust事件统计
     */
    @JavascriptInterface
    fun adjustTrackEvent(eventToken: String) {
        Log.v(TAG, "adjustTrackEvent:${eventToken}")
        val adjustEvent = AdjustEvent(eventToken)
        Adjust.trackEvent(adjustEvent)
    }

    /**
     * 打开paytm
     */
    @JavascriptInterface
    fun openPayTm(data: String) {
        Log.v(TAG, "openPayTm:${data}")
        if (mContext is WebActivity) {
            mContext.openPayTm(data)
        }
    }

    /**
     * 头像获取
     *
     * @param callbackMethod
     */
    @JavascriptInterface
    fun takePortraitPicture(callbackMethod: String) {
        Log.v(TAG, "takePortraitPicture:${callbackMethod}")
        if (mContext is WebActivity) {
            mContext.takePortraitPicture(callbackMethod)
        }
    }

    /**
     * 是否禁用系统返回键
     * 1 禁止
     */
    @JavascriptInterface
    fun shouldForbidSysBackPress(forbid: Int) {
        Log.v(TAG, "shouldForbidSysBackPress:${forbid}")
        if (mContext is WebActivity) {
            mContext.setShouldForbidBackPress(forbid)
        }
    }

    /**
     * 返回键调用h5控制
     *
     * @param forbid 是否禁止返回键 1 禁止
     * @param methodName 反回时调用的h5方法 例如:detailBack() 不需要时传空串只禁止返回
     */
    @JavascriptInterface
    fun forbidBackForJS(forbid: Int, methodName: String) {
        Log.v(TAG, "shouldForbidSysBackPress:${forbid},name=${methodName}")
        if (mContext is WebActivity) {
            mContext.setShouldForbidBackPress(forbid)
            mContext.setBackPressJSMethod(methodName)
        }
    }

    /**
     * 使用手机里面的浏览器打开 url
     *
     * @param url 打开 url
     */
    @JavascriptInterface
    fun openBrowser(url: String) {
        if (mContext is WebActivity) {
            val uri = Uri.parse(url)
            val intent = Intent()
            intent.action = "android.intent.action.VIEW"
            intent.data = uri
            if (intent.resolveActivity(mContext.packageManager) != null) {
                mContext.startActivity(intent)
            }
        }
    }

    @JavascriptInterface
    fun shareInAndroid(type: String, data: String) {
        Log.v(TAG, "shareInAndroid:${data}");
        val shareData: ShareData = Gson().fromJson<ShareData>(data, ShareData::class.java)
        if (mContext is WebActivity) {
            when (type) {
                "facebook" -> {
                    mContext.shareToFacebook(shareData);
                }
                "whatsapp" -> {
                    mContext.shareToWhatsApp(shareData);
                }
                else -> {
                }
            }
        }
    }

    /**
     * 关闭web页面

     */
    @JavascriptInterface
    fun finishWeb() {
        if (mContext is Activity) {
            mContext.finish()
        }
    }

    /**
     * 控制显示当前页面是否显示 TitleBar
     *
     * @param visible
     */
    @JavascriptInterface
    fun showTitleBar(visible: Boolean) {
        if (mContext is WebActivity) {
            val v = if (visible) View.VISIBLE else View.GONE
            mContext.getTitleBar().visibility = v
        }
    }

    /**
     * 新开页面，并显示原生 TitleBar
     *
     * @param url, 新页面 url，title 使用网页里面 title 标签
     */
    @JavascriptInterface
    fun newPageWithTitleBar(url: String) {
        newPageWithTitleBar(url, null)
    }

    /**
     * 新开页面，并显示原生 TitleBar
     *
     * @param url   新页面 url
     * @param title TitleBar 上的 title，但是如果网页里面存在 title 标签优先使用 title 标签
     */
    @JavascriptInterface
    fun newPageWithTitleBar(url: String, title: String?) {
        newPageWithTitleBar(url, title, true)
    }

    /**
     * 新开页面，并控制是否需要原生 TitleBar
     *
     * @param url         新页面 url
     * @param title       TitleBar 上的 title, 但是如果网页里面存在 title 标签优先使用 title 标签
     * @param hasTitleBar 是否需要原生 TitleBar
     */
    @JavascriptInterface
    fun newPageWithTitleBar(url: String, title: String?, hasTitleBar: Boolean) {
        if (mContext is WebActivity) {
            val launcher = Launcher.with(mContext, WebActivity::class.java)
            title?.let {
                launcher.putExtra(WebActivity.EX_TITLE, title)
            }
            when {
                url.contains("app.appsflyer.com") -> {
                    val replaceUrl = url
                        .replace("{android_id}", AppInfo.getAndroidId(mContext))
                        .replace("{advertising_id}", Preference.get().Gaid)
                        .replace("{imei}", AppInfo.getImei(mContext))
                    launcher.putExtra(WebActivity.EX_URL, replaceUrl)
                        .putExtra(WebActivity.EX_HAS_TITLE_BAR, hasTitleBar).execute()
                }
                url.contains("app.adjust.com") -> {
                    Log.e("wtf", url)
                    val replaceUrl = url
                        .replace("{network_androidId_macro}", AppInfo.getAndroidId(mContext))
                        .replace("{network_gaid_macro}", Preference.get().Gaid)
                    Log.e("wtf", replaceUrl)
                    launcher.putExtra(WebActivity.EX_URL, replaceUrl)
                        .putExtra(WebActivity.EX_HAS_TITLE_BAR, hasTitleBar).execute()
                }
                else -> launcher.putExtra(WebActivity.EX_URL, url)
                    .putExtra(WebActivity.EX_HAS_TITLE_BAR, hasTitleBar)
                    .execute()
            }
        }
    }

    @JavascriptInterface
    fun updateTitleText(titleContent: String) {
        if (mContext is WebActivity) {
            val activity = mContext
            activity.runOnUiThread { activity.updateTitleText(titleContent) }
        }
    }

    companion object {
        const val NET_NONE = 0
        const val NET_WIFI = 1
        const val NET_2G = 2
        const val NET_3G = 3
        const val NET_4G = 4
        const val TAG = "AppJs"
    }

    /**
     * 获取当前可用网络的类型
     *
     * @return  * 0 无可用网络
     *  * 1 wifi
     *  * 2 2g
     *  * 3 3g
     *  * 4 4g
     */
    val availableNetwork: Int
        @JavascriptInterface
        get() {
            var availableNetwork = NET_NONE

            val connectivityManager = Utils.context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo

            if (networkInfo == null || !networkInfo.isConnected) {
                return availableNetwork
            }

            if (networkInfo.type == ConnectivityManager.TYPE_WIFI) {
                availableNetwork = NET_WIFI
            } else if (networkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                val subType = networkInfo.subtype

                if (subType == TelephonyManager.NETWORK_TYPE_CDMA
                    || subType == TelephonyManager.NETWORK_TYPE_GPRS
                    || subType == TelephonyManager.NETWORK_TYPE_EDGE
                ) {
                    availableNetwork = NET_2G

                } else if (subType == TelephonyManager.NETWORK_TYPE_UMTS
                    || subType == TelephonyManager.NETWORK_TYPE_HSDPA
                    || subType == TelephonyManager.NETWORK_TYPE_EVDO_A
                    || subType == TelephonyManager.NETWORK_TYPE_EVDO_0
                    || subType == TelephonyManager.NETWORK_TYPE_EVDO_B
                ) {
                    availableNetwork = NET_3G
                } else if (subType == TelephonyManager.NETWORK_TYPE_LTE) {
                    availableNetwork = NET_4G
                }
            }
            return availableNetwork
        }
}

