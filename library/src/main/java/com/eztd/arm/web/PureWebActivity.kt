package com.eztd.arm.web

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import androidx.core.content.ContextCompat
import com.eztd.arm.R
import com.eztd.arm.base.BaseActivity
import com.eztd.arm.tools.Network
import kotlinx.android.synthetic.main.activity_web.*

class PureWebActivity : BaseActivity() {
    companion object {
        const val INFO_HTML_META =
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no\">"
    }

    private var mLoadSuccess: Boolean = false
    private var mPageUrl: String? = null
    private var mTitle: String? = null
    private var mTitleTextColor: String? = null
    private var rewriteTitle: Boolean = true
    private var mHasTitleBar: Boolean = true
    private var webBack: Boolean = true
    private var mPureHtml: String? = null
    private var titleBg: String? = null
    private var titleFieldColor: String? = null
    private var mNetworkChangeReceiver: BroadcastReceiver? = null
    private var mWebViewClient: MyWebViewClient? = null

    private var mPostData: String? = null

    private val isNeedViewTitle: Boolean
        get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pure_web)
        findViewById<View>(R.id.ivBack).setOnClickListener { onBackPressed() }
        mNetworkChangeReceiver = NetworkReceiver()
        mLoadSuccess = true
        initData(intent)
        setStatusBar()
        initTitleBar()
        initWebView()
    }

    private fun initTitleBar() {
        titleBar.visibility = if (mHasTitleBar) View.VISIBLE else View.GONE
        titleBar.setBackgroundColor(
            if (!titleBg.isNullOrBlank()) Color.parseColor(titleBg) else ContextCompat.getColor(
                this,
                R.color.colorPrimary
            )
        )
        webTitle.setTextColor(
            if (!mTitleTextColor.isNullOrBlank()) Color.parseColor(mTitleTextColor) else ContextCompat.getColor(
                this,
                R.color.white
            )
        )
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            if (webBack) {
                webView.goBack()
            } else {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun initData(intent: Intent) {
        titleBg = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_STATE_BAR_BG)
        titleFieldColor = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_STATE_BAR_FIELD_COLOR)
        mHasTitleBar = intent.getBooleanExtra(com.eztd.arm.ExtraKeys.EX_HAS_TITLE_BAR, false)
        mTitle = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_TITLE)
        mTitleTextColor = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_TITLE_TEXT_COLOR)
        rewriteTitle = intent.getBooleanExtra(com.eztd.arm.ExtraKeys.EX_REWRITE_TITLE, true)
        mPageUrl = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_URL)
        mPureHtml = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_HTML)
        mPostData = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_POST_DATA)
        webBack = intent.getBooleanExtra(com.eztd.arm.ExtraKeys.EX_WEB_BACK, true)
    }

    private fun setStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val window = window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor =
                if (!titleBg.isNullOrBlank()) Color.parseColor(titleBg) else getColor(R.color.colorPrimary)
            var systemUiVisibility = window.decorView.systemUiVisibility
            if (!titleFieldColor.isNullOrBlank()) {
                systemUiVisibility = if ("black" == titleFieldColor) {
                    systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
            window.decorView.systemUiVisibility = systemUiVisibility
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(false)
        }
        // init webSettings
        webView.settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setAppCacheEnabled(true)
            setAppCachePath(externalCacheDir?.path ?: "")
            allowFileAccess = true

            // performance improve
            domStorageEnabled = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            loadWithOverviewMode = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }

        webView.apply {
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            } else {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
            mWebViewClient = MyWebViewClient()
            webViewClient = mWebViewClient
            webChromeClient = myWebChromeClient.apply { setActivity(this@PureWebActivity) }
            setDownloadListener { url, _, _, _, _ ->
                val uri = Uri.parse(url)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
        }
        loadPage()
    }

    private fun loadPage() {
        updateTitleText(mTitle)
        if (!TextUtils.isEmpty(mPageUrl)) {
            if (TextUtils.isEmpty(mPostData)) {
                webView.loadUrl(mPageUrl)
            } else {
                webView.postUrl(mPageUrl, mPostData!!.toByteArray())
            }
        } else if (!TextUtils.isEmpty(mPureHtml)) {
            openWebView(mPureHtml!!)
        } else if (TextUtils.isEmpty(mPureHtml)) {
            progressbar.visibility = View.GONE
        }
    }

    private fun openWebView(urlData: String) {
        val content = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            "$INFO_HTML_META<body>$urlData</body>"
        } else {
            getHtmlData("<body>$urlData</body>")
        }
        webView.loadDataWithBaseURL(null, content, "text/html", "utf-8", null)
    }

    private fun getHtmlData(bodyHTML: String): String {
        val head =
            "<head><style>img{max-width: 100%; width:auto; height: auto;}</style>$INFO_HTML_META</head>"
        return "<html>$head$bodyHTML</html>"
    }

    private fun updateTitleText(titleContent: String?) {
        if (isNeedViewTitle) {
            mTitle = titleContent
            webTitle.text = mTitle
            webTitle.isSelected = true
        }
    }

    private inner class MyWebViewClient : android.webkit.WebViewClient() {
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            Log.w(TAG, "onReceivedError$errorCode,$description,$failingUrl")
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            mLoadSuccess = true
            mPageUrl = url
            if (!Network.isNetworkAvailable && TextUtils.isEmpty(mPureHtml)) {
                mLoadSuccess = false
                webView.stopLoading()
            }
        }

        override fun onReceivedSslError(
            webView: WebView,
            sslErrorHandler: SslErrorHandler,
            sslError: SslError
        ) {
            AlertDialog.Builder(this@PureWebActivity)
                .setMessage(R.string.notification_error_ssl_cert_invalid)
                .setPositiveButton("Continue") { dialog, _ ->
                    sslErrorHandler.proceed()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    sslErrorHandler.cancel()
                    dialog.dismiss()
                }
                .create()
                .show()
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            if (isNeedViewTitle) {
                val titleText = view.title
                if (!TextUtils.isEmpty(titleText) && !url.contains(titleText) && rewriteTitle) {
                    mTitle = titleText
                }
                webTitle.text = mTitle
                webTitle.isSelected = true
            } else {
                webTitle.text = mTitle
                webTitle.isSelected = true
            }
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            super.onReceivedError(view, request, error)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val requestUrl = request.url.toString()
                if (mPageUrl.equals(
                        requestUrl,
                        ignoreCase = true
                    ) && error.errorCode <= ERROR_UNKNOWN
                ) {
                    mLoadSuccess = false
                }
            }
        }
    }

    private var myWebChromeClient = object : MyWebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (newProgress >= 99) {
                if (loadingView.visibility == View.VISIBLE) {
                    loadingView.visibility = View.GONE
                }
                progressbar.visibility = View.GONE
            } else {
                if (progressbar.visibility == View.GONE) {
                    progressbar.visibility = View.VISIBLE
                }
                progressbar.progress = newProgress
            }
        }
    }

    private inner class NetworkReceiver : Network.NetworkChangeReceiver() {
        override fun onNetworkChanged(availableNetworkType: Int) {
            if (availableNetworkType > Network.NET_NONE && !mLoadSuccess) {
                if (webView != null) {
                    webView.reload()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPostResume() {
        super.onPostResume()
        mNetworkChangeReceiver?.let { Network.registerNetworkChangeReceiver(this, it) }
    }

    override fun onPause() {
        super.onPause()
        mNetworkChangeReceiver?.let { Network.unregisterNetworkChangeReceiver(this, it) }
        webView.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        myWebChromeClient.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        destroy(webView)
        super.onDestroy()
    }

    /**
     * destroy WebView
     * @param webView the WebView that needs to destroy
     */
    private fun destroy(webView: WebView?) {
        webView?.let { web ->
            web.stopLoading()
            web.parent?.let { (it as ViewGroup).removeView(webView) }
            web.removeAllViews()
            web.destroy()
        }
    }
}