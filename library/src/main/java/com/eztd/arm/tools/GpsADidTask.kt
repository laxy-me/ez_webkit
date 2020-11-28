package com.eztd.arm.tools

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient

/**
 * 通过AsyncTask获取 gps adid
 * @author yangguangda
 * @date 2020/3/31
 */
class GpsADidTask : AsyncTask<Context, Void, String>() {
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
        Log.v("GetGpsIdTask", result)
        Preference.get().gpsAdid = result
    }
}