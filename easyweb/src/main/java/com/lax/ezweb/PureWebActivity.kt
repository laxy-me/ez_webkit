package com.lax.ezweb

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
import com.lax.ezweb.base.BaseActivity
import kotlinx.android.synthetic.main.activity_pure_web.*

/**
 *
 * @author yangguangda
 * @date 2020/9/9
 */
class PureWebActivity : BaseActivity() {
    companion object {
        const val INFO_HTML_META =
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no\">"

        //状态栏背景色
        const val EX_STATE_BAR_BG = "backgroundCol"

        //状态栏字体颜色 black|white
        const val EX_STATE_BAR_FIELD_COLOR = "fieldCol"

        //标题栏字体颜色
        const val EX_TITLE_TEXT_COLOR = "titleColor"
        const val EX_HAS_TITLE_BAR = "hasTitleBar"
        const val EX_TITLE = "title"

        //是否通过url重写title
        const val EX_REWRITE_TITLE = "rewrite_title"

        //加载的Url地址
        const val EX_URL = "url"
        const val EX_POST_DATA = "post_data"
        const val EX_HTML = "html"
        const val EX_WEB_BACK = "web_back"
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
        setContentView(R.layout.web)
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
        titleBg = intent.getStringExtra(EX_STATE_BAR_BG)
        titleFieldColor = intent.getStringExtra(EX_STATE_BAR_FIELD_COLOR)
        mHasTitleBar = intent.getBooleanExtra(EX_HAS_TITLE_BAR, false)
        mTitle = intent.getStringExtra(EX_TITLE)
        mTitleTextColor = intent.getStringExtra(EX_TITLE_TEXT_COLOR)
        rewriteTitle = intent.getBooleanExtra(EX_REWRITE_TITLE, true)
        mPageUrl = intent.getStringExtra(EX_URL)
        mPureHtml = intent.getStringExtra(EX_HTML)
        mPostData = intent.getStringExtra(EX_POST_DATA)
        webBack = intent.getBooleanExtra(EX_WEB_BACK, true)
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
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setAppCacheEnabled(true);
        webSettings.setAppCachePath(externalCacheDir?.path ?: "")
        webSettings.allowFileAccess = true

        // performance improve
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webSettings.setEnableSmoothTransition(true)
        webSettings.domStorageEnabled = true
        webSettings.useWideViewPort = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.loadWithOverviewMode = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.clearHistory()
        webView.clearCache(true)
        webView.clearFormData()
        webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        webView.isDrawingCacheEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        mWebViewClient = MyWebViewClient()
        webView.webViewClient = mWebViewClient
        webChromeClient.setActivity(this)
        webView.webChromeClient = webChromeClient
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
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
        val content: String
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            webView.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
            content = "$INFO_HTML_META<body>$urlData</body>"
        } else {
            webView.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
            content = getHtmlData("<body>$urlData</body>")
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
            super.onReceivedError(view, errorCode, description, failingUrl)
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

    private var webChromeClient = object : MyWebChromeClient() {
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
        webChromeClient.onActivityResult(requestCode, resultCode, data)
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