package com.lax.ezweb.plugin

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.gson.Gson
import com.lax.ezweb.WebActivity
import com.lax.ezweb.data.model.GoogleLoginResponse
import com.lax.ezweb.data.model.GoogleLoginToken
import com.lax.ezweb.data.model.LoginGoogleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.Reader
import java.lang.StringBuilder
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

class GoogleLoginPlugin {

    private lateinit var gso: GoogleSignInOptions
    private lateinit var googleSignInClient: GoogleSignInClient
    private var host = ""
    private var sign = ""
    private var activity = WeakReference<Activity>(null)

    companion object {
        private const val GOOGLE_LOGIN = 46507

        private var ins: GoogleLoginPlugin? = null

        @JvmStatic
        fun getInstance(): GoogleLoginPlugin {
            if (ins == null) {
                synchronized(GoogleLoginPlugin::class.java) {
                    if (ins == null) {
                        ins = GoogleLoginPlugin()
                    }
                }
            }
            return ins!!
        }

        @JvmStatic
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
            if (requestCode == GOOGLE_LOGIN) {
                val signedInAccountFromIntent = GoogleSignIn.getSignedInAccountFromIntent(data)
                val signInAccount = signedInAccountFromIntent.getResult(ApiException::class.java)
                ins?.processGoogleLogin(signInAccount)
            }
            return requestCode == GOOGLE_LOGIN
        }
    }


    fun googleLogin(activity: Activity, data: String?) {
        ins!!.activity = WeakReference(activity)
        val googleData: LoginGoogleData = Gson().fromJson(data, LoginGoogleData::class.java)
        ins!!.host = googleData.host!!
        ins!!.sign = googleData.sign!!
        val acct = GoogleSignIn.getLastSignedInAccount(activity)
        if (acct != null && !acct.isExpired) {
            ins?.processGoogleLogin(acct)
        } else {
            ins?.gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestId()
                .requestProfile()
                .build()
            ins?.googleSignInClient = GoogleSignIn.getClient(activity, ins!!.gso)
            val intent = ins?.googleSignInClient?.signInIntent
            activity.startActivityForResult(intent, GOOGLE_LOGIN)
        }
    }


    private fun processGoogleLogin(signInAccount: GoogleSignInAccount?) {
        if (signInAccount != null) {
            val id = signInAccount.id
            val name = signInAccount.displayName
            handleResult(id ?: "", name ?: "", 1)
        } else {
            Log.e("account", "si为空:" + "\n")
        }
    }

    /**
     *@param type 0:Facebook,1:google
     */
    private fun handleResult(id: String, name: String, type: Int) {
        try {
            runBlocking {
                launch {
                    val response = withContext(Dispatchers.IO) {
                        val url = "${host}/user/google/doLogin2.do" +
                                "?id=${id}&name=${name}&sign=${sign}&type=${type}"
                        try {
                            val conn = URL(url).openConnection()
                            conn.connectTimeout = 5000
                            conn.connect()
                            if ((conn as HttpURLConnection).responseCode in 200..299) {
                                val inputStream = conn.getInputStream()
                                val bufferSize = 1024
                                val buffer = CharArray(bufferSize)
                                val out = StringBuilder()
                                val `in`: Reader =
                                    InputStreamReader(inputStream, "UTF-8")
                                while (true) {
                                    val rsz = `in`.read(buffer, 0, buffer.size)
                                    if (rsz < 0) break
                                    out.append(buffer, 0, rsz)
                                }
                                out.toString()
                            } else {
                                ""
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ""
                        }
                    }
                    if (response.isNotBlank()) {
                        val googleToken: GoogleLoginToken? =
                            Gson().fromJson(response, GoogleLoginResponse::class.java).data
                        val rawCookie: String = createTokenStr(
                            "token1", googleToken?.token1!!
                        ) + "\n" + createTokenStr("token2", googleToken.token2)
                        if (!TextUtils.isEmpty(rawCookie) && !TextUtils.isEmpty(host)) {
                            val cookies = rawCookie.split("\n").toTypedArray()
                            for (cookie in cookies) {
                                CookieManager.getInstance().setCookie(host, cookie)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                CookieManager.getInstance().flush()
                            } else {
                                CookieSyncManager.getInstance().sync()
                            }
                            val act = activity.get()
                            act?.runOnUiThread {
                                if (act is WebActivity) {
                                    act.getWebView().loadUrl(URLDecoder.decode(googleToken.url))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e("account", "si异常:\n$e")
        }
    }

    private fun createTokenStr(name: String, value: String): String {
        return "$name=\"$value\";expires=1; path=/"
    }
}