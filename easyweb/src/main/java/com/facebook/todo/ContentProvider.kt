package com.facebook.todo

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.facebook.todo.tools.ToastUtil
import io.branch.referral.Branch


/**
 * Library中三方库初始化
 * @author yangguangda
 * @date 2020/3/31
 */
@Keep
class ContentProvider : ContentProvider() {
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
                initBranch(application)
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
                Preference.get().fcmToken = token ?: ""
            })
    }

    private fun initFacebook(application: Application) {
        AppEventsLogger.activateApp(application)
    }

    private fun initBranch(application: Application) {
        Branch.getAutoInstance(application)
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