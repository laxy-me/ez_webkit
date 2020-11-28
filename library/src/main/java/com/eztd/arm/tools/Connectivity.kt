package com.eztd.arm.tools

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import com.eztd.arm.provider.ContentProvider


object Connectivity {
    const val NET_NONE = 0
    const val NET_WIFI = 1
    const val NET_2G = 2
    const val NET_3G = 3
    const val NET_4G = 4

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = ContentProvider.autoContext!!
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnectedOrConnecting
        } else {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }

    private fun availableNetwork(): Int {
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
        activity?.apply {
            registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }
    }

    fun unregisterNetworkChangeReceiver(activity: Activity?, receiver: BroadcastReceiver) {
        activity?.apply {
            try {
                unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) { // throw when receiver not register
                e.printStackTrace()
            }
        }
    }

    abstract class NetworkChangeReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action!!.equals(ConnectivityManager.CONNECTIVITY_ACTION, ignoreCase = true)) {
                val availableNetworkType = availableNetwork()
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
