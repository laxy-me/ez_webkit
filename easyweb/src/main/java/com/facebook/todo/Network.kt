package com.facebook.todo

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.telephony.TelephonyManager

object Network {

    val isNetworkAvailable: Boolean
        get() {
            val connectivityManager = ContentProvider.autoContext!!
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val networkInfo = connectivityManager.activeNetworkInfo

            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }

    const val NET_NONE = 0
    const val NET_WIFI = 1
    const val NET_2G = 2
    const val NET_3G = 3
    const val NET_4G = 4

    private val availableNetwork: Int
        get() {
            var availableNetwork = NET_NONE

            val connectivityManager = ContentProvider.autoContext!!
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo ?: return availableNetwork

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

    fun registerNetworkChangeReceiver(activity: Activity?, receiver: BroadcastReceiver) {
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        activity?.registerReceiver(receiver, filter)
    }

    fun unregisterNetworkChangeReceiver(activity: Activity?, receiver: BroadcastReceiver) {
        if (activity != null) {
            try {
                activity.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) { // throw when receiver not register
                e.printStackTrace()
            }

        }
    }

    abstract class NetworkChangeReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action!!.equals(ConnectivityManager.CONNECTIVITY_ACTION, ignoreCase = true)) {
                val availableNetworkType = availableNetwork
                onNetworkChanged(availableNetworkType)
            }
        }

        /**
         * NET_NONE, NET_WIFI, NET_2G, NET_3G, NET_4G
         *
         * @param availableNetworkType
         */
        protected abstract fun onNetworkChanged(availableNetworkType: Int)
    }
}
