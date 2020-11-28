package com.eztd.arm.web

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
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.eztd.arm.R
import com.eztd.arm.base.BaseActivity
import com.eztd.arm.base.Launcher
import com.eztd.arm.dialog.AppDialog
import com.eztd.arm.dialog.ImageSelectController
import com.eztd.arm.tools.*
import com.eztd.arm.tools.plugin.FacebookLoginPlugin
import com.eztd.arm.tools.plugin.GoogleLoginPlugin
import com.eztd.arm.tools.plugin.PayTmPlugin
import com.eztd.arm.tools.plugin.SharePlugin
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import kotlinx.android.synthetic.main.activity_web.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

abstract class WebActivity : BaseActivity(), CoroutineScope {
    private val infoHtmlMeta =
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no\">"
    private val schemeSms = "sms:"
    private val schemeIntent = "intent://"
    private val openSysContent: Int = 11113

    private var loadStatus: Boolean = false
    private var pageUrl: String? = null
    private var mTitle: String? = null
    private var refreshTitle: Boolean = true
    private var mHasTitleBar: Boolean = false
    private var htmlCode: String? = null
    private var titleBg: String? = null
    private var titleFieldColor: String? = null
    private var adUrl: String = ""
    private var adContent: String = ""
    private var adTime: Int = 5

    private var networkChangeReceiver: BroadcastReceiver? = null
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

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var job: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        setContentView(R.layout.activity_web)
        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }
        networkChangeReceiver = NetworkReceiver()
        loadStatus = true
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
                        .putExtra(com.eztd.arm.ExtraKeys.EX_TITLE, "AD")
                        .putExtra(com.eztd.arm.ExtraKeys.EX_HAS_TITLE_BAR, true)
                        .putExtra(com.eztd.arm.ExtraKeys.EX_URL, adContent)
                        .execute()
                }
            }
            skipCountDown.setOnClickListener {
                closeAd()
            }
            launch(Dispatchers.Main) {
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
            launch(Dispatchers.Main) {
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
        networkChangeReceiver?.let { Connectivity.registerNetworkChangeReceiver(this, it) }
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
                    webView.evaluateJavascript(javaScript, null)
                }
                return
            }
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun initData(intent: Intent) {
        var data = intent.getParcelableExtra<TestData>("data")
        Log.e("wtf", "aaa=${data == null}")
        mTitle = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_TITLE)
        pageUrl = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_URL)
        titleFieldColor = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_TITLE_FIELD_COLOR)
        titleBg = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_TITLE_BG)
        htmlCode = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_HTML)
        mPostData = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_POST_DATA)
        refreshTitle = intent.getBooleanExtra(com.eztd.arm.ExtraKeys.EX_REWRITE_TITLE, true)
        mHasTitleBar = intent.getBooleanExtra(com.eztd.arm.ExtraKeys.EX_HAS_TITLE_BAR, true)
        adUrl = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_AD_URL) ?: ""
        adContent = intent.getStringExtra(com.eztd.arm.ExtraKeys.EX_AD_CONTENT) ?: ""
        adTime = intent.getIntExtra(com.eztd.arm.ExtraKeys.EX_AD_TIME, 5)
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
        pageUrl?.let { url ->
            if (!url.startsWith("http")) { // http or https
                pageUrl = "http://$url"
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        // init cookies
        syncCookies(pageUrl)
        webView.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(false)
            }
            clearHistory()
            isDrawingCacheEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            addJavascriptInterface(JsBridge(this@WebActivity), "AppJs")
            webViewClient = MyWebViewClient()
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                setRenderPriority(WebSettings.RenderPriority.NORMAL)

                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true

                cacheMode = WebSettings.LOAD_DEFAULT
                databaseEnabled = true
                setAppCacheEnabled(true)
                setAppCachePath(externalCacheDir?.path)
                allowFileAccess = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
            }
        }
        loadPage()
    }

    private fun loadPage() {
        updateTitleText(mTitle)
        if (!TextUtils.isEmpty(pageUrl)) {
            if (TextUtils.isEmpty(mPostData)) {
                webView.loadUrl(pageUrl)
            } else {
                webView.postUrl(pageUrl, mPostData!!.toByteArray())
            }
        } else if (!TextUtils.isEmpty(htmlCode)) {
            openWebView(htmlCode!!)
        } else if (TextUtils.isEmpty(htmlCode)) {
            progressbar.visibility = View.GONE
        }
    }

    private fun openWebView(urlData: String) {
        val content: String = getHtmlData("<body>$urlData</body>")
        webView.loadDataWithBaseURL(null, content, "text/html", "utf-8", null)
    }

    private fun getHtmlData(bodyHTML: String): String {
        val head =
            "<head><style>img{max-width: 100%; width:auto; height: auto;}</style>$infoHtmlMeta</head>"
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
                Manifest.permission.READ_EXTERNAL_STORAGE
            ), intArrayOf(), object : AfterPermissionGranted {
                override fun permissionGranted() {
                    ImageDownloadTask().execute(url)
                }
            }
        )
    }

    private fun syncCookies(pageUrl: String?) {
        val rawCookie: String? = MyCookieManger.getInstance().lastCookie
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
            if (pageUrl.equals(
                    failingUrl,
                    ignoreCase = true
                ) && errorCode <= ERROR_UNKNOWN
            ) {
                loadStatus = false
            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            val requestUrl = request.url.toString()
            if (pageUrl.equals(
                    requestUrl,
                    ignoreCase = true
                ) && error.errorCode <= ERROR_UNKNOWN
            ) {
                loadStatus = false
            }
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            resetForbid()
            loadStatus = true
            pageUrl = url
            if (!Connectivity.isNetworkAvailable() && TextUtils.isEmpty(htmlCode)) {
                loadStatus = false
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
                if (!TextUtils.isEmpty(titleText) && !url.contains(titleText) && refreshTitle) {
                    mTitle = titleText
                }
                webTitle.text = mTitle
                webTitle.isSelected = true
            } else {
                webTitle.text = mTitle
                webTitle.isSelected = true
            }
        }

//        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
//            return onShouldOverrideUrlLoading(url)
//        }
//
//        @TargetApi(Build.VERSION_CODES.N)
//        override fun shouldOverrideUrlLoading(
//            view: WebView,
//            request: WebResourceRequest
//        ): Boolean {
//            return onShouldOverrideUrlLoading(request.url?.toString() ?: "")
//        }
    }

    private fun handleCommonLink(url: String): Boolean {
        if (url.startsWith(WebView.SCHEME_TEL)
            || url.startsWith(schemeSms)
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
            if (TextUtils.isEmpty(intentUrl) || !intentUrl.startsWith(schemeIntent)) {
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
        when {
            handleCommonLink(url) -> {
                return true
            }
            url.startsWith(schemeIntent) -> {
                handleIntentUrl(url)
                return true
            }
            url.startsWith("market://") -> {
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                if ("google" == AppInfo.getMetaData(this, "CHANNEL")) {
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
            }
            else -> return false
        }
    }

    private lateinit var mCallbackMethodName: String

    fun takePortraitPicture(callbackMethod: String) {
        requestPermission(
            arrayOf(
                Manifest.permission.CAMERA,
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
            override fun takePhoto(dialog: AppDialog) {
                mCallbackMethodName = callbackMethod
                PictureSelector.create(this@WebActivity)
                    .openCamera(PictureMimeType.ofImage())
                    .imageEngine(GlideEngine.createGlideEngine())
                    .forResult(PictureConfig.CHOOSE_REQUEST)
                dialog.dismiss()
            }

            override fun takeFromGallery(dialog: AppDialog) {
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
        AppDialog.solo(this)
            .setCustomViewController(customViewController)
            .setWindowGravity(Gravity.BOTTOM)
            .setWidthScale(1f)
            .show()
    }

    private fun callback2Web(str: String?) {
        Log.d(TAG, "callback2Web: " + str?.length)
        if (!TextUtils.isEmpty(mCallbackMethodName)) {
            val builder = StringBuilder(mCallbackMethodName).append("(")
            builder.append("'").append("data:image/png;base64,$str").append("'")
            builder.append(")")
            val methodName = builder.toString()
            val javaScript = "javascript:$methodName"
            Log.e("wtf", javaScript)
            webView.evaluateJavascript(javaScript, null)
        }
    }

    private inner class NetworkReceiver : Connectivity.NetworkChangeReceiver() {
        override fun onNetworkChanged(availableNetworkType: Int) {
            if (availableNetworkType > Connectivity.NET_NONE && !loadStatus) {
                if (webView != null) {
                    webView.reload()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        PayTmPlugin.onActivityResult(requestCode, resultCode, data)
        FacebookLoginPlugin.onActivityResult(requestCode, resultCode, data)
        GoogleLoginPlugin.onActivityResult(requestCode, resultCode, data)
        SharePlugin.onActivityResult(requestCode, resultCode, data)
        myWebChromeClient.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQ_LOGIN_CODE ->
                    // init cookies
                    webView.postDelayed({
                        syncCookies(pageUrl)
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
        if (resultCode == openSysContent) {
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
        val cursor = contentResolver.query(uri, projection, null, null, null)
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
        networkChangeReceiver?.let { Connectivity.unregisterNetworkChangeReceiver(this, it) }
        webView.onPause()
    }

    override fun onDestroy() {
        destroy(webView)
        job.cancel()
        GoogleLoginPlugin.onDetach()
        FacebookLoginPlugin.onDetach()
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