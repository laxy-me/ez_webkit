package com.eztd.arm.web

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import com.eztd.arm.base.Launcher
import com.eztd.arm.provider.ContentProvider
import com.eztd.arm.tools.AppInfo
import com.eztd.arm.tools.Preference
import com.eztd.arm.tools.plugin.FacebookLoginPlugin
import com.eztd.arm.tools.plugin.GoogleLoginPlugin
import com.eztd.arm.tools.plugin.PayTmPlugin
import com.eztd.arm.tools.plugin.SharePlugin
import com.eztd.arm.util.MethodUtil
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.analytics.FirebaseAnalytics
import io.branch.referral.util.BranchEvent
import org.json.JSONObject

open class JsBridge(private val mContext: Context) {

    /**
     * 获取设备号
     */
    @JavascriptInterface
    fun getDeviceId(): String? {
        var deviceHardwareId = AppInfo.getDeviceHardwareId(mContext)
        if (deviceHardwareId.isEmpty()) {
            deviceHardwareId = Preference.get().gpsAdid
        }
        Log.v(TAG, "getDeviceId:${deviceHardwareId}")
        return deviceHardwareId
    }

    /**
     * getAppCode
     */
    @JavascriptInterface
    fun getAppCode(): String {
        Log.v(TAG, "getAppCode:")
        return ""
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
     * 获取fcm 令牌
     */
    @JavascriptInterface
    fun takeFCMPushId(): String {
        Log.v(TAG, "takeFCMPushId:${Preference.get().fcmToken}")
        return Preference.get().fcmToken
    }

    /**
     * 获取渠道
     */
    @JavascriptInterface
    fun takeChannel(): String {
        Log.v(TAG, "takeChannel:${AppInfo.getMetaData(mContext, "CHANNEL")}")
        return AppInfo.getMetaData(mContext, "CHANNEL")
    }

    /**
     * 就是设备id
     */
    @JavascriptInterface
    fun getGoogleId(): String {
        val androidId = AppInfo.getAndroidId(mContext)
        Log.v(TAG, "getGoogleId:$androidId")
        return androidId
    }

    /**
     * 谷歌广告id
     */
    @JavascriptInterface
    fun getGaId(): String? {
        Log.v(TAG, "getGaId:${Preference.get().gpsAdid}")
        return Preference.get().gpsAdid
    }

    /**
     * 调取谷歌登录方法
     */
    @JavascriptInterface
    fun openGoogle(data: String) {
        if (mContext is WebActivity) {
            Log.v(TAG, "openGoogle:${data}")
            GoogleLoginPlugin.getInstance().googleLogin(mContext, data)
        }
    }

    /**
     * 调取facebook登录方法
     */
    @JavascriptInterface
    fun loginFacebook(data: String) {
        if (mContext is WebActivity) {
            Log.v(TAG, "loginFacebook:${data}")
            FacebookLoginPlugin.getInstance().facebookLogin(mContext, data)
        }
    }

    /**
     * branch事件统计
     * @param eventName 统计事件名称
     */
    @JavascriptInterface
    fun branchEvent(eventName: String) {
        Log.v(TAG, "branchEvent:\neventName:${eventName}")
        BranchEvent(eventName)
            .logEvent(mContext)
    }

    /**
     * branch事件统计
     * @param eventName 统计时间名称
     * @param parameters 自定义统计参数
     */
    @JavascriptInterface
    fun branchEvent(eventName: String, parameters: String) {
        Log.v(TAG, "branchEvent:\neventName:${eventName}\nparameters:$parameters")
        val branchEvent = BranchEvent(eventName)
        val obj = JSONObject(parameters)
        val bundle = Bundle()
        for (key in obj.keys()) {
            val value = obj.optString(key)
            bundle.putString(key, value)
            branchEvent.addCustomDataProperty(
                key,
                value
            )
        }
        branchEvent
            .logEvent(mContext)
    }

    /**
     * branch事件统计
     * @param eventName 统计事件名称
     * @param parameters 自定义统计参数
     * @param alias 事件别名
     */
    @JavascriptInterface
    fun branchEvent(eventName: String, parameters: String, alias: String) {
        Log.v(TAG, "branchEvent:\neventName:${eventName}\nparameters:$parameters\nalias$alias")
        val branchEvent = BranchEvent(eventName)
        val obj = JSONObject(parameters)
        val bundle = Bundle()
        for (key in obj.keys()) {
            val value = obj.optString(key)
            bundle.putString(key, value)
            branchEvent.addCustomDataProperty(
                key,
                value
            )
        }
        branchEvent
            .setCustomerEventAlias(alias)
            .logEvent(mContext)
    }

    /**
     * facebook事件统计
     * @param eventName 事件名称
     * @param valueToSum 计数数值
     * @param parameters 自定义统计参数json{}需要全是String类型
     */
    @JavascriptInterface
    fun facebookEvent(eventName: String, valueToSum: Double, parameters: String) {
        Log.v(
            TAG,
            "facebookEvent:\neventName:${eventName}\nvalueToSum:$valueToSum\nparameters:$parameters"
        )
        val logger = AppEventsLogger.newLogger(ContentProvider.autoContext)
        val obj = JSONObject(parameters)
        val bundle = Bundle()
        for (key in obj.keys()) {
            val value = obj.optString(key)
            bundle.putString(key, value)
        }
        logger.logEvent(eventName, valueToSum, bundle)
    }

    /**
     * facebook事件统计
     * @param eventName 事件名称
     * @param parameters 自定义统计参数json{}需要全是String类型
     */
    @JavascriptInterface
    fun facebookEvent(eventName: String, parameters: String) {
        Log.v(TAG, "facebookEvent:\neventName:${eventName}\nparameters:$parameters")
        val logger = AppEventsLogger.newLogger(ContentProvider.autoContext)
        val obj = JSONObject(parameters)
        val bundle = Bundle()
        for (key in obj.keys()) {
            val value = obj.optString(key)
            bundle.putString(key, value)
        }
        logger.logEvent(eventName, bundle)
    }

    /**
     * facebook计数统计
     * @param eventName 事件名称
     * @param valueToSum 计数数值
     */
    @JavascriptInterface
    fun facebookEvent(eventName: String, valueToSum: Double) {
        Log.v(TAG, "facebookEvent:\neventName:${eventName}\nvalueToSum:$valueToSum")
        val logger = AppEventsLogger.newLogger(ContentProvider.autoContext)
        logger.logEvent(eventName, valueToSum)
    }

    /**
     * facebook 计数事件统计
     * @param eventName 事件名称
     */
    @JavascriptInterface
    fun facebookEvent(eventName: String) {
        Log.v(TAG, "facebookEvent:\neventName:${eventName}")
        val logger = AppEventsLogger.newLogger(ContentProvider.autoContext)
        logger.logEvent(eventName)
    }

    /**
     * firebase事件统计
     */
    @JavascriptInterface
    fun firebaseEvent(category: String, parameters: String) {
        Log.v(TAG, "firebaseEvent:\ncategory:${category}\nparameters:$parameters")
        val obj = JSONObject(parameters)
        val bundle = Bundle()
        for (key in obj.keys()) {
            val value = obj.optString(key)
            bundle.putString(key, value)
        }
        FirebaseAnalytics.getInstance(mContext).logEvent(category, bundle)
    }

    /**
     * 打开paytm
     */
    @JavascriptInterface
    fun openPayTm(data: String) {
        Log.v(TAG, "openPayTm:${data}")
        if (mContext is WebActivity) {
            PayTmPlugin.openPayTm(data, mContext)
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
     * 是否存在交互方法
     *
     * @param name 方法名
     */
    @JavascriptInterface
    fun isContainsName(callbackMethod: String, name: String) {
        var has = false
        Log.v(TAG, "isContainsName:${callbackMethod};${name}")
        val classMethods = MethodUtil.getClassMethods(JsBridge::class.java)
        classMethods?.let {
            for (method in it) {
                if (method != null) {
                    has = method.name == name
                    if (has) {
                        return@let
                    }
                }
            }
        }
        if (mContext is WebActivity) {
            mContext.runOnUiThread {
                val webView = mContext.getWebView()
                val javaScript = "javascript:$callbackMethod('$has')"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(javaScript, null)
                } else {
                    webView.loadUrl(javaScript)
                }
            }
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
        Log.e(TAG, "openBrowser url$url")
        if (mContext is WebActivity) {
            val uri = Uri.parse(url)
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = uri
            }
            if (intent.resolveActivity(mContext.packageManager) != null) {
                mContext.startActivity(intent)
            }
        }
    }

    /**
     * 打开一个基本配置的web url
     *
     * @param json 打开web传参 选填
     * {"title":"", 打开时显示的标题
     *  "url":"", 加载的地址
     *  "hasTitleBar":false, 是否显示标题栏
     *  "rewriteTitle":"true", 是否通过加载的Web重写标题
     *  "stateBarTextColor":"black", 状态栏字体颜色 black|white
     *  "titleTextColor":"#FFFFFF", 标题字体颜色
     *  "titleColor":"#FFFFFF", 标题背景色
     *  "postData":"", webView post方法时需要传参
     *  "html":"", 加载htmlCode,
     *  "webBack":"true", true:web回退|false 直接关闭页面
     * }
     */
    @JavascriptInterface
    fun openPureBrowser(json: String) {
        Log.v(TAG, "openPureBrowser json:$json")
        if (mContext is WebActivity) {
            val jsonObject = JSONObject(json)
            Launcher.with(mContext, PureWebActivity::class.java)
                .putExtra(
                    com.eztd.arm.ExtraKeys.EX_STATE_BAR_BG,
                    jsonObject.optString("titleColor")
                )
                .putExtra(
                    com.eztd.arm.ExtraKeys.EX_STATE_BAR_FIELD_COLOR,
                    jsonObject.optString("stateBarTextColor")
                )
                .putExtra(
                    com.eztd.arm.ExtraKeys.EX_TITLE_TEXT_COLOR,
                    jsonObject.optString("titleTextColor")
                )
                .putExtra(com.eztd.arm.ExtraKeys.EX_TITLE, jsonObject.optString("title"))
                .putExtra(
                    com.eztd.arm.ExtraKeys.EX_HAS_TITLE_BAR,
                    jsonObject.optBoolean("hasTitleBar")
                )
                .putExtra(
                    com.eztd.arm.ExtraKeys.EX_REWRITE_TITLE,
                    jsonObject.optBoolean("rewriteTitle")
                )
                .putExtra(com.eztd.arm.ExtraKeys.EX_URL, jsonObject.optString("url"))
                .putExtra(com.eztd.arm.ExtraKeys.EX_POST_DATA, jsonObject.optString("postData"))
                .putExtra(com.eztd.arm.ExtraKeys.EX_HTML, jsonObject.optString("html"))
                .putExtra(com.eztd.arm.ExtraKeys.EX_WEB_BACK, jsonObject.optString("webBack"))
                .execute()
        }
    }

    @JavascriptInterface
    fun shareInAndroid(type: String, data: String) {
        Log.v(TAG, "shareInAndroid:${data}")
        if (mContext is WebActivity) {
            when (type) {
                "facebook" -> {
                    FacebookLoginPlugin.getInstance().shareToFacebook(mContext, data)
                }
                "whatsapp" -> {
                    SharePlugin.getInstance().shareToWhatsApp(mContext, data)
                }
                else -> {
                }
            }
        }
    }

    @JavascriptInterface
    fun launchAppDetail() {
        Log.v(TAG, "launchAppDetail")
        AppInfo.launchAppDetail(mContext, mContext.packageName, null)
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
        Log.v(TAG, "showTitleBar:${visible}")
        if (mContext is WebActivity) {
            mContext.runOnUiThread {
                val v = if (visible) View.VISIBLE else View.GONE
                mContext.getTitleBar().visibility = v
            }
        }
    }

    /**
     * 新开页面，并显示原生 TitleBar
     *
 hge    * @param url, 新页面 url，title 使用网页里面 title 标签
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
     * 获取app版本号
     */
    @JavascriptInterface
    open fun getVersionName(): String? {
        return if (mContext is WebActivity) {
            AppInfo.getVersionName(mContext)
        } else ""
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
                launcher.putExtra(com.eztd.arm.ExtraKeys.EX_TITLE, title)
            }
            when {
                else -> launcher.putExtra(com.eztd.arm.ExtraKeys.EX_URL, url)
                    .putExtra(com.eztd.arm.ExtraKeys.EX_HAS_TITLE_BAR, hasTitleBar)
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

            val connectivityManager = ContentProvider.autoContext!!
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

