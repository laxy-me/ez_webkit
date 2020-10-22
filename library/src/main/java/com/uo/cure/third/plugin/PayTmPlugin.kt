package com.uo.cure.third.plugin

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.uo.cure.ExtraKeys
import com.uo.cure.base.Launcher
import com.uo.cure.provider.ContentProvider
import com.uo.cure.web.WebActivity
import org.json.JSONObject


class PayTmPlugin {
    companion object {
        private const val PAYTM_APP_PACKAGE: String = "net.one97.paytm"
        private val TAG = PayTmPlugin::class.java.simpleName
        private const val PAYTM_REQUEST_CODE = 11112

        //生产环境
        private const val URL_PAYTM_PRODUCTION: String =
            "https://securegw.paytm.in/theia/api/v1/showPaymentPage"

        //测试环境
        const val URL_PAYTM_TEST: String =
            "https://securegw-stage.paytm.in/theia/api/v1/showPaymentPage"

        //Check current Paytm app version
        private fun getPaytmVersion(context: Context): String? {
            val pm: PackageManager = context.packageManager
            try {
                val pkgInfo: PackageInfo =
                    pm.getPackageInfo(PAYTM_APP_PACKAGE, PackageManager.GET_ACTIVITIES)
                return pkgInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                Log.i(TAG, "Paytm app not installed")
            }
            return null
        }

        private fun versionCompare(str1: String, str2: String): Int {
            if (TextUtils.isEmpty(str1) || TextUtils.isEmpty(str2)) {
                return 1
            }
            val vals1 = str1.split("\\.".toRegex()).toTypedArray()
            val vals2 = str2.split("\\.".toRegex()).toTypedArray()
            var i = 0
            //set index to first non-equal ordinal or length of shortest version string
            while (i < vals1.size && i < vals2.size && vals1[i]
                    .equals(vals2[i], ignoreCase = true)
            ) {
                i++
            }
            //compare first non-equal ordinal number
            if (i < vals1.size && i < vals2.size) {
                val diff = Integer.valueOf(vals1[i])
                    .compareTo(Integer.valueOf(vals2[i]))
                return Integer.signum(diff)
            }
            //the strings are equal or one string is a substring of the other
            //e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
            return Integer.signum(vals1.size - vals2.size)
        }

        @JvmStatic
        fun openPayTm(data: String, activity: Activity) {
            val payInfo = JSONObject(data)
            try {
                if (versionCompare(
                        getPaytmVersion(activity.applicationContext) ?: "",
                        "8.6.0"
                    ) < 0
                ) {
                    val bundle = Bundle()
                    bundle.putDouble("nativeSdkForMerchantAmount", payInfo.optDouble("amount"))
                    bundle.putString("orderid", payInfo.optString("orderId"))
                    bundle.putString("txnToken", payInfo.optString("textToken"))
                    bundle.putString("mid", payInfo.optString("mid"))
                    val paytmIntent = Intent()
                    paytmIntent.component =
                        ComponentName(PAYTM_APP_PACKAGE, "net.one97.paytm.AJRJarvisSplash")
                    // You must have to pass hard coded 2 here, Else your transaction would not proceed.
                    paytmIntent.putExtra("paymentmode", 2)
                    paytmIntent.putExtra("bill", bundle)
                    activity.startActivityForResult(paytmIntent, PAYTM_REQUEST_CODE)
                } else {
                    val paytmIntent = Intent()
                    paytmIntent.component = ComponentName(
                        PAYTM_APP_PACKAGE,
                        "net.one97.paytm.AJRRechargePaymentActivity"
                    )
                    paytmIntent.putExtra("paymentmode", 2)
                    paytmIntent.putExtra("enable_paytm_invoke", true)
                    paytmIntent.putExtra("paytm_invoke", true)
                    paytmIntent.putExtra(
                        "price",
                        payInfo.optDouble("amount").toString()
                    ) //this is string amount

                    paytmIntent.putExtra("nativeSdkEnabled", true)
                    paytmIntent.putExtra("orderid", payInfo.optString("orderId"))
                    paytmIntent.putExtra("txnToken", payInfo.optString("textToken"))
                    paytmIntent.putExtra("mid", payInfo.optString("mid"))
                    activity.startActivityForResult(paytmIntent, PAYTM_REQUEST_CODE)
                }
            } catch (e: ActivityNotFoundException) {
                val postData = StringBuilder()
                val postUrl =
                    URL_PAYTM_PRODUCTION + "?mid=" + payInfo.optString("mid") + "&orderId=" + payInfo.optString(
                        "orderId"
                    )
                postData.append("MID=").append(payInfo.optString("mid"))
                    .append("&txnToken=").append(payInfo.optString("textToken"))
                    .append("&ORDER_ID=").append(payInfo.optString("orderId"))
                if (activity is WebActivity) {
                    activity.runOnUiThread {
                        activity.getWebView().postUrl(postUrl, postData.toString().toByteArray())
                    }
                } else {
                    Launcher.with(activity, WebActivity::class.java)
                        .putExtra(ExtraKeys.EX_HAS_TITLE_BAR, true)
                        .putExtra(ExtraKeys.EX_URL, postUrl)
                        .putExtra(ExtraKeys.EX_POST_DATA, postData.toString())
                }
            }
        }

        @JvmStatic
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
            if (requestCode == PAYTM_REQUEST_CODE) {
                val response = data?.getStringExtra("response")
                Log.i(
                    TAG,
                    "PayTmCallback:$response${data?.getStringExtra("nativeSdkForMerchantMessage")}"
                )
                try {
                    val jsonObject = JSONObject(response ?: "{}")
                    val status = jsonObject.optString("STATUS", "")
                    if (status.isNotBlank()) {
                        Toast.makeText(
                            ContentProvider.autoContext,
                            status.replace("TXN_", ""),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                }
            }
            return requestCode == PAYTM_REQUEST_CODE
        }
    }
}