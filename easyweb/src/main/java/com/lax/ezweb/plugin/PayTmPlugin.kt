package com.lax.ezweb.plugin

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.lax.ezweb.Launcher
import com.lax.ezweb.WebActivity
import com.lax.ezweb.data.model.PayTmInfo

class PayTmPlugin {
    companion object {
        private val TAG = PayTmPlugin::class.java.simpleName
        private const val PAYTM_REQUEST_CODE = 11112

        //生产环境
        private const val URL_PAYTM_PRODUCTION: String =
            "https://securegw.paytm.in/theia/api/v1/showPaymentPage"

        //测试环境
        const val URL_PAYTM_TEST: String =
            "https://securegw-stage.paytm.in/theia/api/v1/showPaymentPage"

        @JvmStatic
        fun openPayTm(data: String, activity: Activity) {
            val payInfo: PayTmInfo = Gson().fromJson(data, PayTmInfo::class.java)
            try {
                val bundle = Bundle()
                bundle.putDouble("nativeSdkForMerchantAmount", payInfo.amount)
                bundle.putString("orderid", payInfo.orderId)
                bundle.putString("txnToken", payInfo.textToken)
                bundle.putString("mid", payInfo.mid)
                val paytmIntent = Intent()
                paytmIntent.component =
                    ComponentName("net.one97.paytm", "net.one97.paytm.AJRJarvisSplash")
                // You must have to pass hard coded 2 here, Else your transaction would not proceed.
                paytmIntent.putExtra("paymentmode", 2)
                paytmIntent.putExtra("bill", bundle)
                activity.startActivityForResult(paytmIntent, PAYTM_REQUEST_CODE)
            } catch (e: ActivityNotFoundException) {
                val postData = StringBuilder()
                val postUrl =
                    URL_PAYTM_PRODUCTION + "?mid=" + payInfo.mid + "&orderId=" + payInfo.orderId
                postData.append("MID=").append(payInfo.mid)
                    .append("&txnToken=").append(payInfo.textToken)
                    .append("&ORDER_ID=").append(payInfo.orderId)
                if (activity is WebActivity) {
                    activity.runOnUiThread {
                        activity.getWebView().postUrl(postUrl, postData.toString().toByteArray())
                    }
                } else {
                    Launcher.with(activity, WebActivity::class.java)
                        .putExtra(WebActivity.EX_HAS_TITLE_BAR, true)
                        .putExtra(WebActivity.EX_URL, postUrl)
                        .putExtra(WebActivity.EX_POST_DATA, postData.toString())
                }
            }
        }

        @JvmStatic
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
            Log.i(TAG, "PayTmCallback:" + data?.getStringExtra("response"))
            return requestCode == PAYTM_REQUEST_CODE
        }
    }
}