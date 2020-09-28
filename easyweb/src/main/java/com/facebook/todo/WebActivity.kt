package com.facebook.todo

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.LinearLayout
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.facebook.todo.base.BaseActivity
import com.facebook.todo.data.CookieManger
import com.facebook.todo.dialog.ImageSelectController
import com.facebook.todo.dialog.SmartDialog
import com.facebook.todo.dowload.ImageDownloadTask
import com.facebook.todo.plugin.FacebookPlugin
import com.facebook.todo.plugin.GoogleLoginPlugin
import com.facebook.todo.plugin.PayTmPlugin
import com.facebook.todo.plugin.SharePlugin
import com.facebook.todo.tools.AppInfo
import com.facebook.todo.tools.ImageUtil
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import kotlinx.android.synthetic.main.web.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URISyntaxException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Matcher
import java.util.regex.Pattern

@Keep
open class WebActivity : BaseActivity() {
    companion object {
        const val INFO_HTML_META =
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no\">"

        const val EX_TITLE_BG = "backgroundCol"
        const val EX_TITLE_FIELD_COLOR = "fieldCol"
        const val EX_TITLE = "title"
        const val EX_HAS_TITLE_BAR = "hasTitleBar"
        const val EX_REWRITE_TITLE = "rewrite_title"
        const val EX_URL = "url"
        const val EX_POST_DATA = "post_data"
        const val EX_HTML = "html"
        const val EX_AD_URL = "ad_url"
        const val EX_AD_CONTENT = "ad_content"
        const val EX_AD_TIME = "ad_time"

        const val SCHEME_SMS = "sms:"
        const val INTENT_SCHEME = "intent://"
        const val OPEN_SYS_CONTENT: Int = 11113
    }

    private var mLoadSuccess: Boolean = false
    private var mPageUrl: String? = null
    private var mTitle: String? = null
    private var rewriteTitle: Boolean = true
    private var mHasTitleBar: Boolean = true
    private var mPureHtml: String? = null
    private var titleBg: String? = null
    private var titleFieldColor: String? = null
    private var adUrl: String = ""
    private var adContent: String = ""
    private var adTime: Int = 5

    private var mNetworkChangeReceiver: BroadcastReceiver? = null
    private var mWebViewClient: MyWebViewClient? = null
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

    private var mPostData: String? = null

    private val isNeedViewTitle: Boolean
        get() = true

    fun getTitleBar(): LinearLayout {
        return titleBar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web)
        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }
        mNetworkChangeReceiver = NetworkReceiver()
        mLoadSuccess = true
        initData(intent)
        setStatusBar()
        initTitleBar()
        tryToFixPageUrl()
        setVisibleState()
        initWebView()
    }

    fun getWebView(): WebView {
        return webView
    }

    private fun setVisibleState() {
        if (adUrl.isBlank()) {
            closeAd()
        } else {
            showAd()
            Glide
                .with(this)
                .load(adUrl)
                .thumbnail(0.3f)
                .into(ad)
            ad.setOnClickListener {
                if (!adContent.isBlank()) {
                    Launcher.with(this, WebActivity::class.java)
                        .putExtra(EX_TITLE, "AD")
                        .putExtra(EX_HAS_TITLE_BAR, true)
                        .putExtra(EX_URL, adContent)
                        .execute()
                }
            }
            skipCountDown.setOnClickListener {
                closeAd()
            }
            GlobalScope.launch(Dispatchers.Main) {
                var time: Int
                for (i in adTime downTo 0) { // 从 adTime 到 1 的倒计时
                    time = i
                    delay(1000)
                    // 更新文本
                    skipCountDown.text = getString(R.string.count_down_text, time)
                    if (i == 0) {
                        closeAd()
                    }
                }
            }
            GlobalScope.launch(Dispatchers.Main) {
                for (i in 10 downTo 0) { // 从 adTime 到 1 的倒计时
                    if (i == 0) {
                        if (loadingView.visibility == View.VISIBLE) {
                            loadingView.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun showAd() {
        adContentView.visibility = View.VISIBLE
        webContentView.visibility = View.INVISIBLE
    }

    private fun closeAd() {
        adContentView.visibility = View.GONE
        webContentView.visibility = View.VISIBLE
    }

    private fun initTitleBar() {
        titleBar.visibility = if (mHasTitleBar) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPostResume() {
        super.onPostResume()
        mNetworkChangeReceiver?.let { Network.registerNetworkChangeReceiver(this, it) }
    }

    private var mForbid: Int = 0
    fun resetForbid() {
        mForbid = 0
        mCallbackMethodName = ""
    }

    fun setShouldForbidBackPress(forbid: Int) {
        mForbid = forbid
    }

    fun setBackPressJSMethod(methodName: String) {
        mCallbackMethodName = methodName
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            if (mForbid == 1) {
                if (!TextUtils.isEmpty(mCallbackMethodName)) {
                    val javaScript = "javascript:$mCallbackMethodName"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        webView.evaluateJavascript(javaScript, null)
                    } else {
                        webView.loadUrl(javaScript)
                    }
                }
                return
            }
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun initData(intent: Intent) {
        mTitle = intent.getStringExtra(EX_TITLE)
        mPageUrl = intent.getStringExtra(EX_URL)
        titleFieldColor = intent.getStringExtra(EX_TITLE_FIELD_COLOR)
        titleBg = intent.getStringExtra(EX_TITLE_BG)
        mPureHtml = intent.getStringExtra(EX_HTML)
        mPostData = intent.getStringExtra(EX_POST_DATA)
        rewriteTitle = intent.getBooleanExtra(EX_REWRITE_TITLE, true)
        mHasTitleBar = intent.getBooleanExtra(EX_HAS_TITLE_BAR, true)
        adUrl = intent.getStringExtra(EX_AD_URL) ?: ""
        adContent = intent.getStringExtra(EX_AD_CONTENT) ?: ""
        adTime = intent.getIntExtra(EX_AD_TIME, 5)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun setStatusBar() {
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor =
                if (!titleBg.isNullOrBlank()) Color.parseColor(titleBg) else getColor(R.color.colorPrimary)
            var systemUiVisibility = window.decorView.systemUiVisibility
            if (!titleFieldColor.isNullOrBlank()) {
                systemUiVisibility = if ("black" == titleFieldColor) {
                    systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
            decorView.systemUiVisibility = systemUiVisibility
        }
    }

    private fun tryToFixPageUrl() {
        mPageUrl?.let { url ->
            if (!url.startsWith("http")) { // http or https
                mPageUrl = "http://$url"
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        // init cookies
        syncCookies(mPageUrl)
        webView.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(false)
            }
            clearHistory()
            clearCache(true)
            clearFormData()
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            addJavascriptInterface(AppJs(this@WebActivity), "AppJs")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            } else {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }

            mWebViewClient = MyWebViewClient()
            webViewClient = mWebViewClient
            webChromeClient = myWebChromeClient.apply { setActivity(this@WebActivity) }
            setOnLongClickListener {
                val result = webView.hitTestResult
                if (result != null) {
                    val type = result.type
                    if (type == WebView.HitTestResult.IMAGE_TYPE) {
                        showSaveImageDialog(result)
                    }
                }
                false
            }
            setDownloadListener { url, _, _, _, _ ->
                val uri = Uri.parse(url)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
            // init webSettings
            settings.apply {
                var userAgentString = this.userAgentString
                userAgentString = getString(R.string.android_web_agent, userAgentString)
                this.userAgentString = userAgentString

                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                //mWebView.getSettings().setAppCacheEnabled(true);
                //webSettings.setAppCachePath(getExternalCacheDir().getPath());
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
        val content: String = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
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

    private fun showSaveImageDialog(result: WebView.HitTestResult) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.save_image))
            .setCancelable(false)
            .setMessage(getString(R.string.save_image_to_local))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                val url = result.extra
                saveImage(url)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show().apply {
                getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this@WebActivity, R.color.colorPrimary))
                getButton(DialogInterface.BUTTON_NEGATIVE)
                    .setTextColor(
                        ContextCompat.getColor(
                            this@WebActivity,
                            R.color.textColorPrimary
                        )
                    )
            }
    }

    /**
     * 保存图片到相册
     */
    private fun saveImage(url: String?) {
        requestPermission(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), intArrayOf(), object : AfterPermissionGranted {
                override fun permissionGranted() {
                    ImageDownloadTask().execute(url)
                }
            }
        )
    }

    private fun syncCookies(pageUrl: String?) {
        val rawCookie: String? = CookieManger.getInstance().lastCookie
        Log.d(TAG, "syncCookies: $rawCookie, $pageUrl")
        if (!TextUtils.isEmpty(rawCookie) && !TextUtils.isEmpty(pageUrl)) {
            CookieManager.getInstance().setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().acceptThirdPartyCookies(webView)
            }
            val cookies =
                rawCookie!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            for (cookie in cookies) {
                CookieManager.getInstance().setCookie(pageUrl, cookie)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().flush()
            }
            Log.i(TAG, "getCookies: " + CookieManager.getInstance().getCookie(pageUrl))
            val sync = !TextUtils.isEmpty(CookieManager.getInstance().getCookie(pageUrl))
            Log.i(TAG, "syncCookies: $sync")
        }
    }

    fun updateTitleText(titleContent: String?) {
        if (isNeedViewTitle) {
            mTitle = titleContent
            webTitle.text = mTitle
            webTitle.isSelected = true
        }
    }

    protected inner class MyWebViewClient : android.webkit.WebViewClient() {
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            Log.w(TAG, "onReceivedError$errorCode,$description,$failingUrl")
            if (mPageUrl.equals(
                    failingUrl,
                    ignoreCase = true
                ) && errorCode <= ERROR_UNKNOWN
            ) {
                mLoadSuccess = false
            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            val requestUrl = request.url.toString()
            if (mPageUrl.equals(
                    requestUrl,
                    ignoreCase = true
                ) && error.errorCode <= ERROR_UNKNOWN
            ) {
                mLoadSuccess = false
            }
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            resetForbid()
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
            AlertDialog.Builder(this@WebActivity)
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

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return onShouldOverrideUrlLoading(url)
        }

        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            return onShouldOverrideUrlLoading(request.url?.toString() ?: "")
        }
    }

    private fun handleCommonLink(url: String): Boolean {
        if (url.startsWith(WebView.SCHEME_TEL)
            || url.startsWith(SCHEME_SMS)
            || url.startsWith(WebView.SCHEME_MAILTO)
            || url.startsWith(WebView.SCHEME_GEO)
        ) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            } catch (ignored: ActivityNotFoundException) {
                ignored.printStackTrace()
            }
            return true
        }
        return false
    }

    private fun handleIntentUrl(intentUrl: String) {
        try {
            if (TextUtils.isEmpty(intentUrl) || !intentUrl.startsWith(INTENT_SCHEME)) {
                return
            }
            if (lookup(intentUrl)) {
                return
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun lookup(url: String): Boolean {
        try {
            val intent: Intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            val info =
                packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            // 跳到该应用
            if (info != null) {
                startActivity(intent)
                return true
            }
        } catch (ignore: Throwable) {
            ignore.printStackTrace()
        }
        return false
    }

    protected fun onShouldOverrideUrlLoading(url: String): Boolean {
        if (url.startsWith("weixin://wap/pay?") || url.startsWith("weixin")
            || url.startsWith("wechat") || url.startsWith("mqq")
            || url.startsWith("mqqapi://") || url.startsWith("mqqwpa")
            || url.startsWith("alipays:") || url.startsWith("alipay")
        ) {
            //打开本地App进行支付
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse(url)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
            return true
        } else if (handleCommonLink(url)) {
            return true
        } else if (url.startsWith(INTENT_SCHEME)) {
            handleIntentUrl(url)
            return true
        } else if (url.startsWith("market://")) {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            if ("google" == AppInfo.getMetaData(this, AppInfo.Meta.CHANNEL)) {
                intent.setPackage("com.android.vending")
            }
            intent.data = Uri.parse(url)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                finish()
            } else if (url.contains("id=")) {
                intent.data = Uri.parse(
                    "https://play.google.com/store/apps/details${
                        url.subSequence(
                            url.indexOf("?"), url.length
                        )
                    }"
                )
                startActivity(intent)
            }
            return true
        } else if (url.startsWith("https://")) {
            try {
                val decode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    URLDecoder.decode(url, StandardCharsets.UTF_8.name())
                } else {
                    URLDecoder.decode(url, "UTF-8")
                }
                val splitArray = decode.split("&")
                if (splitArray.count() > 1 && splitArray[1].contains("url")) {
                    //截取其中的link 进行跳转
                    val uri = splitArray[1].replace("url=", "")
                    val intent = Intent().apply {
                        action = Intent.ACTION_VIEW
                        data = Uri.parse(uri)
                    }
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                        return true
                    }
                }
            } catch (e: Exception) {
            }
        } else if (url.startsWith("intent://")) {
            try {
                val newUrl: String = parseUrlString(url)
                intent.action = Intent.ACTION_VIEW
                intent.data = Uri.parse(newUrl)
                startActivity(intent)
                return true
            } catch (e: Exception) {
            }
        }
        return false
    }

    /**
     * 利用正则表达式，提取出：https://play.google.com/store/apps/details?id%3Dcom.whizdm.moneyview.loans
     */
    private fun parseUrlString(url: String): String {
        val regEx = "(link=)(.*)(#)"
        val p: Pattern = Pattern.compile(regEx)
        val m: Matcher = p.matcher(url)
        var matchString = ""
        if (m.find()) {
            matchString = m.group()
        }
        matchString = matchString.substring(5, matchString.length - 1)
        return unicodeDecode(matchString)
    }

    /**
     * 转义成：https://play.google.com/store/apps/details?id=com.whizdm.moneyview.loans
     */
    private fun unicodeDecode(string: String): String {
        var newString = string
        val pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))")
        val matcher = pattern.matcher(newString)
        var ch: Char
        while (matcher.find()) {
            ch = (matcher.group(2) ?: "").toInt(16).toChar()
            newString = newString.replace(matcher.group(1) ?: "", ch.toString() + "")
        }
        return newString
    }

    private lateinit var mCallbackMethodName: String
    private var mIsBase64: Boolean = true

    fun takePortraitPicture(callbackMethod: String) {
        requestPermission(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), intArrayOf(), object : AfterPermissionGranted {
                override fun permissionGranted() {
                    selectImage(callbackMethod)
                }
            }
        )
    }

    private fun selectImage(callbackMethod: String) {
        val customViewController = ImageSelectController(this)
        customViewController.setOnGetPhotoClickListener(object :
            ImageSelectController.OnGetPhotoClickListener {
            override fun takePhoto(dialog: SmartDialog) {
                mIsBase64 = true
                mCallbackMethodName = callbackMethod
                PictureSelector.create(this@WebActivity)
                    .openCamera(PictureMimeType.ofImage())
                    .imageEngine(GlideEngine.createGlideEngine())
                    .forResult(PictureConfig.CHOOSE_REQUEST)
                dialog.dismiss()
            }

            override fun takeFromGallery(dialog: SmartDialog) {
                mIsBase64 = true
                mCallbackMethodName = callbackMethod
                PictureSelector.create(this@WebActivity)
                    .openGallery(PictureMimeType.ofImage())
                    .imageEngine(GlideEngine.createGlideEngine())
                    .maxSelectNum(1)
                    .isGif(false)
                    .forResult(PictureConfig.CHOOSE_REQUEST)
                dialog.dismiss()
            }
        })
        SmartDialog.solo(this)
            .setCustomViewController(customViewController)
            .setWindowGravity(Gravity.BOTTOM)
            .setWidthScale(1f)
            .show()
    }

    private fun callback2Web(str: String?) {
        Log.d(TAG, "callback2Web: " + str?.length)
        if (!TextUtils.isEmpty(mCallbackMethodName)) {
            val builder = StringBuilder(mCallbackMethodName).append("(")
            if (mIsBase64) {
                builder.append("'").append("data:image/png;base64,$str").append("'")
            } else {
                builder.append("'").append(str).append("'")
            }
            builder.append(")")
            val methodName = builder.toString()
            val javaScript = "javascript:$methodName"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(javaScript, null)
            } else {
                webView.loadUrl(javaScript)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        PayTmPlugin.onActivityResult(requestCode, resultCode, data)
        FacebookPlugin.onActivityResult(requestCode, resultCode, data)
        GoogleLoginPlugin.onActivityResult(requestCode, resultCode, data)
        SharePlugin.onActivityResult(requestCode, resultCode, data)
        myWebChromeClient.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQ_CODE_LOGIN ->
                    // init cookies
                    webView.postDelayed({
                        syncCookies(mPageUrl)
                        webView.reload()
                    }, 200)
                PictureConfig.CHOOSE_REQUEST -> {
                    val selectList = PictureSelector.obtainMultipleResult(data)
                    if (selectList.isNotEmpty()) {
                        val localMedia = selectList[0]
                        val path: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            localMedia.androidQToPath
                        } else {
                            when {
                                localMedia.isCompressed -> {
                                    localMedia.compressPath
                                }
                                localMedia.isCut -> {
                                    localMedia.cutPath
                                }
                                else -> {
                                    localMedia.path
                                }
                            }
                        }
                        if (path.isNotBlank()) {
                            callback2Web(ImageUtil.compressImageToBase64(path))
                        }
                    }
                }
            }
        }
        if (resultCode == OPEN_SYS_CONTENT) {
            val uri = data?.data
            val path = getImagePath(uri)
            if (path.isNotBlank()) {
                callback2Web(ImageUtil.compressImageToBase64(path))
            }
        }
    }

    private fun getImagePath(uri: Uri?): String {
        var path = ""
        if (uri == null) {
            return ""
        }
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return path
    }

    override fun onPause() {
        super.onPause()
        mNetworkChangeReceiver?.let { Network.unregisterNetworkChangeReceiver(this, it) }
        webView.onPause()
    }

    override fun onDestroy() {
        destroy(webView)
        super.onDestroy()
    }

    /**
     * destroy WebView
     * @param webView the WebView that needs to destroy
     */
    open fun destroy(webView: WebView?) {
        webView?.let { web ->
            web.stopLoading()
            web.parent?.let { (it as ViewGroup).removeView(webView) }
            web.removeAllViews()
            web.destroy()
        }
    }
}