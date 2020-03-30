package com.buyluck.booth

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import io.branch.referral.Branch

/**
 *
 * @author yangguangda
 * @date 2020/3/20
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Utils.init(applicationContext)
        Preference.init(applicationContext)
        ToastUtil.init(applicationContext)
        GetGpsIdTask().execute(applicationContext)
        if (BuildConfig.DEBUG) {
            Branch.enableDebugMode()
        }
        Branch.getAutoInstance(this)
    }

    @SuppressLint("StaticFieldLeak")
    inner class GetGpsIdTask : AsyncTask<Context, Void, String>() {
        override fun doInBackground(vararg context: Context): String {
            var id = ""
            try {
                id = AdvertisingIdClient.getAdvertisingIdInfo(context.first()).id
            } catch (e: Exception) {
                Log.v("GetGpsIdTask", e.toString())
            }
            return id
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            Preference.get().Gaid = result
        }
    }
}