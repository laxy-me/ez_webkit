package com.lax.ezweb

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.igexin.sdk.PushManager
import com.lax.ezweb.service.MyPushService
import com.lax.ezweb.service.PushIntentService
import com.lax.ezweb.tools.AppInfo
import com.lax.ezweb.tools.ToastUtil
import com.lax.ezweb.tools.Utils
import io.branch.referral.Branch


/**
 * Library中三方库初始化
 * @author yangguangda
 * @date 2020/3/31
 */
class EzWebInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        if (context != null) {
            val application = context!!.applicationContext
            initAdjust(application as Application)
            Utils.init(application)
            Preference.init(application)
            ToastUtil.init(application)
            GetGpsIdTask().execute(application)
            initBranch(application)
            initPush(application)
        }
        return false
    }

    private fun initPush(application: Context?) {
        PushManager.getInstance()
            .registerPushIntentService(application, PushIntentService::class.java)
        PushManager.getInstance().initialize(application, MyPushService::class.java)
        PushManager.getInstance().setPrivacyPolicyStrategy(application, true)
    }

    private fun initBranch(application: Context) {
        if (BuildConfig.DEBUG) {
            Branch.enableDebugMode()
        }
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