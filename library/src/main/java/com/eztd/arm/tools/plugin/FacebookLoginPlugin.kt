package com.eztd.arm.tools.plugin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import androidx.fragment.app.Fragment
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.share.Sharer
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import com.eztd.arm.web.WebActivity
import kotlinx.coroutines.*
import org.json.JSONException
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

class FacebookLoginPlugin : CoroutineScope {
    private var callbackManager: CallbackManager? = null
    private var shareDialog: ShareDialog? = null
    private var host = ""
    private var sign = ""
    private var activity = WeakReference<Activity>(null)
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    companion object {
        val TAG = FacebookLoginPlugin::class.java.simpleName

        @JvmField
        var ins: FacebookLoginPlugin? = null

        @JvmStatic
        fun getInstance(): FacebookLoginPlugin {
            if (ins == null) {
                synchronized(FacebookLoginPlugin::class.java) {
                    if (ins == null) {
                        ins = FacebookLoginPlugin()
                    }
                }
            }
            return ins!!
        }

        @JvmStatic
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
            return ins?.callbackManager?.onActivityResult(requestCode, resultCode, data) ?: false
        }

        @JvmStatic
        fun onDetach() {
            ins?.job?.cancel()
        }
    }

    fun facebookLogin(activity: Activity, data: String?) {
        ins!!.activity = WeakReference(activity)
        val googleData = JSONObject(data ?: "")
        this.host = googleData.optString("host")
        this.sign = googleData.optString("sign")
        if (callbackManager == null) {
            callbackManager = CallbackManager.Factory.create()
        }
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult?> {
                override fun onSuccess(loginResult: LoginResult?) {
                    loginResult?.accessToken?.let {
                        setFacebookData(loginResult.accessToken)
                    }
                }

                override fun onCancel() {
                }

                override fun onError(exception: FacebookException) {
                }
            })
        val accessToken: AccessToken? = AccessToken.getCurrentAccessToken()
        if (accessToken != null && !accessToken.isExpired) {
            setFacebookData(accessToken)
        } else {
            LoginManager.getInstance().logIn(activity, listOf("public_profile"))
        }
    }

    private fun setFacebookData(accessToken: AccessToken) {
        val request = GraphRequest.newMeRequest(
            accessToken
        ) { _, response ->
            if (response != null) {
                try {
                    Log.i("Response", response.toString())
                    val profile = Profile.getCurrentProfile()
                    handleResult(accessToken.userId ?: "", profile.name ?: "")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
        val parameters = Bundle()
        parameters.putString("fields", "id,email,first_name,last_name,gender")
        request.parameters = parameters
        request.executeAsync()
    }

    /**
     *@param type 0:Facebook,1:google
     */
    private fun handleResult(id: String, name: String, type: Int = 0) {
        val url = "${host}/user/google/doLogin2.do?id=${id}&name=${name}&sign=${sign}&type=${type}"
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
                    googleToken?.apply {
                        val token1 = optString("token1")
                        val token2 = optString("token2")
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
    }

    @Throws(IOException::class, UnsupportedEncodingException::class)
    private fun requestLogin(url: String): String {
        val conn = URL(url).openConnection().apply {
            connectTimeout = 5000
        }
        conn.connect()
        return if ((conn as HttpURLConnection).responseCode in 200..299) {
            val inputStream = conn.getInputStream()
            val bufferSize = 1024
            val buffer = CharArray(bufferSize)
            val out = StringBuilder()
            val `in`: Reader = InputStreamReader(inputStream, "UTF-8")
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

    fun shareToFacebook(activity: Activity, data: String) {
        val shareData = JSONObject(data)
        if (callbackManager == null) {
            callbackManager = CallbackManager.Factory.create()
        }
        shareDialog = ShareDialog(activity)
        val linkContent = ShareLinkContent.Builder()
            .setContentUrl(Uri.parse(shareData.optString("url")))
            .setQuote(shareData.optString("content"))
            .build()
        shareDialog?.registerCallback(
            callbackManager,
            object : FacebookCallback<Sharer.Result> {
                override fun onSuccess(result: Sharer.Result?) {
                    shareCallBack(
                        shareData.optString("domainUrl"),
                        shareData.optString("inviteCode")
                    )
                }

                override fun onCancel() {
                }

                override fun onError(error: FacebookException?) {
                    error?.printStackTrace()
                }
            })
        shareDialog?.show(linkContent)
    }

    fun shareToFacebook(fragment: Fragment, data: String) {
        val shareData = JSONObject(data)
        if (callbackManager == null) {
            callbackManager = CallbackManager.Factory.create()
        }
        shareDialog = ShareDialog(fragment)
        val linkContent = ShareLinkContent.Builder()
            .setContentUrl(Uri.parse(shareData.optString("url")))
            .setQuote(shareData.optString("content"))
            .build()
        shareDialog?.registerCallback(
            callbackManager,
            object : FacebookCallback<Sharer.Result> {
                override fun onSuccess(result: Sharer.Result?) {
                    shareCallBack(
                        shareData.optString("domainUrl"),
                        shareData.optString("inviteCode")
                    )
                }

                override fun onCancel() {
                }

                override fun onError(error: FacebookException?) {
                    error?.printStackTrace()
                }
            })
        shareDialog?.show(linkContent)
    }

    /**
     * 需要app 配合 调用接口 分享链接之后 调用接口
     * /user/userTask/dailyFaceAndWhats.do
     * inviteCode 邀请码
     * type  1:facebook  2 whatsApp
     */
    private fun shareCallBack(domainUrl: String, inviteCode: String, type: Int = 1) {
        runBlocking {
            val response = withContext(Dispatchers.IO) {
                val url =
                    "${domainUrl}/user/userTask/dailyFaceAndWhats.do?inviteCode=${inviteCode}&type=${type}"
                try {
                    val conn = URL(url).openConnection()
                    conn.connectTimeout = 5000
                    conn.connect()
                    (conn as HttpURLConnection).responseCode
                } catch (e: Exception) {
                    e.printStackTrace()
                    500
                }
            }
            if (response in 200..299) {
                Log.e(TAG, "share success")
                val activity = activity.get()
                if (activity is WebActivity) {
                    activity.getWebView().reload()
                }
            }
        }
    }
}