package com.eztd.arm.third.plugin

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.eztd.arm.web.WebActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.io.UnsupportedEncodingException
import java.lang.StringBuilder
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Throws

class GoogleLoginPlugin : CoroutineScope {
    private lateinit var gso: GoogleSignInOptions
    private lateinit var googleSignInClient: GoogleSignInClient
    private var host = ""
    private var sign = ""
    private var activity = WeakReference<Activity>(null)
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

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
            if (requestCode == GOOGLE_LOGIN && data != null) {
                val signedInAccountFromIntent = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val signInAccount =
                        signedInAccountFromIntent.getResult(ApiException::class.java)
                    ins?.processGoogleLogin(signInAccount)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return requestCode == GOOGLE_LOGIN
        }

        @JvmStatic
        fun onDetach() {
            ins?.apply {
                job.cancel()
            }
        }
    }

    fun googleLogin(activity: Activity, data: String?) {
        ins!!.activity = WeakReference(activity)
        val googleData = JSONObject(data ?: "")
        ins!!.host = googleData.optString("host")
        ins!!.sign = googleData.optString("sign")
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
            val email = signInAccount.email
            handleResult(id ?: "", name ?: "", email ?: "")
        } else {
            Log.e("account", "si为空:" + "\n")
        }
    }

    /**
     *@param type 0:Facebook,1:google
     */
    private fun handleResult(id: String, name: String, email: String, type: Int = 1) {
        val url =
            "${host}/user/google/doLogin2.do?id=${id}&name=${name}&email=${email}&sign=${sign}&type=${type}"
        try {
            launch {
                val response = withContext(Dispatchers.IO) {
                    try {
                        requestLogin(url)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ""
                    }
                }
                if (response.isNotBlank()) {
                    val googleToken = JSONObject(response).optJSONObject("data")
                    Log.v("google login",googleToken.toString())
                    googleToken?.apply {
                        val token1 = optString("token1");
                        val token2 = optString("token2");
                        if (token1.isNotBlank() && token2.isNotBlank()) {
                            syncCookie(token1, token2)
                        }
                        val optString = optString("url")
                        loadUrl(optString)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e("account", "si异常:\n$e")
        }
    }

    private fun loadUrl(url: String) {
        val act = activity.get()
        if (act is WebActivity) {
            val decode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                URLDecoder.decode(url, StandardCharsets.UTF_8.name())
            } else {
                URLDecoder.decode(url, "UTF-8")
            }
            act.getWebView().loadUrl(decode)
        }
    }

    private fun syncCookie(token1: String, token2: String) {
        val rawCookie: String =
            createTokenStr("token1", token1) + "\n" +
                    createTokenStr("token2", token2)
        val cookies = rawCookie.split("\n").toTypedArray()
        for (cookie in cookies) {
            CookieManager.getInstance().setCookie(host, cookie)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().flush()
        } else {
            CookieSyncManager.getInstance().sync()
        }
        Log.e("cookie", CookieManager.getInstance().toString())
    }

    @Throws(IOException::class, UnsupportedEncodingException::class)
    private fun requestLogin(url: String): String {
        val conn = URL(url).openConnection()
        conn.connectTimeout = 5000
        conn.connect()
        return if ((conn as HttpURLConnection).responseCode in 200..299) {
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
    }

    private fun createTokenStr(name: String, value: String): String {
        return "$name=\"$value\";expires=1; path=/"
    }

    /**
     * 退出登陆
     */
    fun signOut() {
        ins?.googleSignInClient?.signOut()
    }
}