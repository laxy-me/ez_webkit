package com.lax.ezweb

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.igexin.sdk.PushManager
import com.lax.ezweb.service.MyPushService
import com.lax.ezweb.service.PushIntentService
import com.lax.ezweb.tools.AppInfo
import com.lax.ezweb.tools.ToastUtil
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import io.branch.referral.Branch


/**
 * Library中三方库初始化
 * @author yangguangda
 * @date 2020/3/31
 */
class EzWebInitProvider : ContentProvider() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @JvmField
        var autoContext: Context? = null
    }

    override fun onCreate(): Boolean {
        autoContext = context
        if (context != null) {
            try {
                val application = context!!.applicationContext as Application
                Preference.init(application)
                ToastUtil.init(application)
                GetGpsIdTask().execute(application)
                initAdjust(application)
                initBranch(application)
                initPush(application)
                initUm(application)
                initFacebook(application)
                initFCM()
            } catch (e: Exception) {
                e.stackTrace
            }
        }
        return false
    }

    private fun initFCM() {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("fcm", "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }
                // Get new Instance ID token
                val token = task.result?.token
                Log.d("fcm", token)
                Preference.get().pushId = token ?: ""
            })
    }

    private fun initFacebook(application: Application) {
        AppEventsLogger.activateApp(application)
    }

    private fun initUm(application: Application) {
        val um = AppInfo.getMetaData(application, "UMENG_APP_KEY")
        if (um.isNotBlank()) {
            UMConfigure.setLogEnabled(BuildConfig.DEBUG)
            UMConfigure.init(application, UMConfigure.DEVICE_TYPE_PHONE, "")
            // 选用AUTO页面采集模式
            MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO);
        }
    }

    private fun initPush(application: Application) {
        val pushAppId = AppInfo.getMetaData(application, "PUSH_APPID")
        if (pushAppId.isNotBlank()) {
            //for google play store
            PushManager.getInstance()
                .registerPushIntentService(application, PushIntentService::class.java)
            PushManager.getInstance().initialize(application, MyPushService::class.java)
            PushManager.getInstance().setPrivacyPolicyStrategy(application, true)
            //2.14.0.0 for other markets
//            PushManager.getInstance().initialize(context)
        }
    }

    private fun initBranch(application: Application) {
        Branch.getAutoInstance(application)
    }

    private fun initAdjust(application: Application) {
        val appToken = AppInfo.getMetaData(application, "ADJUST_APPTOKEN")
        if (appToken.isNotBlank()) {
            val environment = AdjustConfig.ENVIRONMENT_PRODUCTION
            val config = AdjustConfig(application, appToken, environment, false)
            val adjustTracker = AppInfo.getMetaData(application, "ADJUST_TRACK_TOKEN")
            if (adjustTracker.isNotBlank()) {
                config.setDefaultTracker(adjustTracker)
            }
            Adjust.onCreate(config)
            application.registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())
        }
    }

    private class AdjustLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            Adjust.onResume()
        }

        override fun onActivityPaused(activity: Activity) {
            Adjust.onPause()
        }

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}

        override fun onActivityDestroyed(activity: Activity) {}

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {}
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun getType(uri: Uri): String? {
        return null
    }
}