package com.buyluck.booth

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.buyluck.booth.dialog.ImageSelectController
import com.buyluck.booth.dialog.SmartDialog
import com.buyluck.booth.model.PayTmInfo
import com.buyluck.booth.model.ShareData
import com.buyluck.permission.Permission
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.share.Sharer
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import com.google.gson.Gson
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureConfig
import com.luck.picture.lib.config.PictureMimeType
import com.songbai.apparms.utils.ToastUtil
import kotlinx.android.synthetic.main.web.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder


open class WebActivity : BaseActivity() {
    companion object {
        const val TAG = "WebActivity"
        const val INFO_HTML_META =
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no\">"

        const val SHARE_RESULT_CODE = 11111

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

        const val PAYTM_REQUEST_CODE = 11112
        //生产环境
        const val URL_PAYTM_PRODUCTION: String =
            "https://securegw.paytm.in/theia/api/v1/showPaymentPage"
        //测试环境
        const val URL_PAYTM_TEST: String =
            "https://securegw-stage.paytm.in/theia/api/v1/showPaymentPage"
    }

    private var mLoadSuccess: Boolean = false
    protected var mPageUrl: String? = null
    protected var mTitle: String? = null
    protected var rewriteTitle: Boolean = true
    protected var mHasTitleBar: Boolean = true
    protected var mPureHtml: String? = null
    protected var titleBg: String? = null
    protected var titleFieldColor: String? = null
    protected var adUrl: String = ""
    protected var adContent: String = ""
    protected var adTime: Int = 5

    private var mNetworkChangeReceiver: BroadcastReceiver? = null
    private var mWebViewClient: MyWebViewClient? = null

    private var mPostData: String? = null

    private var callbackManager: CallbackManager? = null
    private var shareDialog: ShareDialog? = null
    private var shareData: ShareData? = null

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
                Launcher.with(this, WebActivity::class.java)
                    .putExtra(EX_TITLE, "AD")
                    .putExtra(EX_HAS_TITLE_BAR, true)
                    .putExtra(EX_URL, adContent)
                    .execute()
            }
            skipAd.setOnClickListener {
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
        Network.registerNetworkChangeReceiver(this, mNetworkChangeReceiver!!)
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

    private fun tryToFixPageUrl() {
        if (!TextUtils.isEmpty(mPageUrl)) {
            if (!(mPageUrl ?: "").startsWith("http")) { // http or https
                mPageUrl = "http://$mPageUrl"
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected fun initWebView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(false)
        }

        // init cookies
        syncCookies(mPageUrl)

        // init webSettings
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        var userAgentString = webSettings.userAgentString
        userAgentString = getString(R.string.android_web_agent) + " " + userAgentString
        webSettings.userAgentString = userAgentString
        //mWebView.getSettings().setAppCacheEnabled(true);l
        //webSettings.setAppCachePath(getExternalCacheDir().getPath());
        webSettings.allowFileAccess = true

        // performance improve
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webSettings.setEnableSmoothTransition(true)
        webSettings.domStorageEnabled = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.clearHistory()
        webView.clearCache(true)
        webView.clearFormData()
        webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        webView.isDrawingCacheEnabled = true
        webView.addJavascriptInterface(AppJs(this), "AppJs")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        mWebViewClient = MyWebViewClient()
        webView.webViewClient = mWebViewClient
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress >= 99) {
                    progressbar.visibility = View.GONE
                } else {
                    if (progressbar.visibility == View.GONE) {
                        progressbar.visibility = View.VISIBLE
                    }
                    progressbar.progress = newProgress
                }
            }
        }
        webView.setOnLongClickListener {
            val result = webView.hitTestResult
            if (result != null) {
                val type = result.type
                if (type == WebView.HitTestResult.IMAGE_TYPE) {
                    showSaveImageDialog(result)
                }
            }
            false
        }
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
        loadPage()
    }

    protected fun loadPage() {
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

    private fun showSaveImageDialog(result: WebView.HitTestResult) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.save_image))
            .setCancelable(false)
            .setMessage(getString(R.string.save_image_to_local))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                val url = result.extra
                //保存图片到相册
                saveImage(url)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.sixtyPercentWhite))
    }

    @Permission(
        permissions = [
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE]
    )
    private fun saveImage(url: String?) {
        DownloadTask().execute(url)
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

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return if (onShouldOverrideUrlLoading(view, url)) {
                true
            } else super.shouldOverrideUrlLoading(view, url)
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            return if (onShouldOverrideUrlLoading(view, request.url?.toString() ?: "")) {
                true
            } else super.shouldOverrideUrlLoading(view, request)
        }
    }

    protected fun onShouldOverrideUrlLoading(view: WebView, url: String): Boolean {
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
        } else if (url.startsWith("market://")) {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            if ("google" == AppInfo.getMetaData(this, AppInfo.Meta.UMENG_CHANNEL)) {
                intent.setPackage("com.android.vending")
            }
            intent.data = Uri.parse(url)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                finish()
            } else if (url.contains("id=")) {
                intent.data = Uri.parse(
                    "https://play.google.com/store/apps/details${url.subSequence(
                        url.indexOf("?"), url.length
                    )}"
                )
                startActivity(intent)
            }
            return true
        } else if (url.startsWith("https://")) {
            //截取其中的link 进行跳转
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            val decode = URLDecoder.decode(url, null)
            val splitArray = decode.split("&")
            if (splitArray.count() > 1 && splitArray[1].contains("url")) {
                val uri = splitArray[1].replace("url=", "")
                intent.data = Uri.parse(uri)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                    return true
                }
            }
        }
        return false
    }

    private lateinit var mCallbackMethodName: String
    private var mIsBase64: Boolean = true
    private var mArg1: String? = null

    @Permission(
        permissions = [
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE]
    )
    fun takePortraitPicture(callbackMethod: String) {
        val customViewController = ImageSelectController(this)
        customViewController.setOnGetPhotoClickListener(object :
            ImageSelectController.OnGetPhotoClickListener {
            override fun takePhoto(dialog: SmartDialog) {
                mIsBase64 = true
                mCallbackMethodName = callbackMethod
                mArg1 = null
                PictureSelector.create(this@WebActivity)
                    .openCamera(PictureMimeType.ofImage())
                    .forResult(PictureConfig.CHOOSE_REQUEST)
                dialog.dismiss()
            }

            override fun takeFromGallery(dialog: SmartDialog) {
                mIsBase64 = true
                mCallbackMethodName = callbackMethod
                mArg1 = null
                PictureSelector.create(this@WebActivity)
                    .openGallery(PictureMimeType.ofImage())
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

    private fun callback2Web(str: String) {
        Log.d(TAG, "callback2Web: " + str.length)
        if (!TextUtils.isEmpty(mCallbackMethodName)) {
            val builder = StringBuilder(mCallbackMethodName).append("(")
            if (mIsBase64) {
                if (TextUtils.isEmpty(mArg1)) {
                    builder.append("'").append("data:image/png;base64,$str").append("'")
                } else {
                    builder.append("'").append(mArg1).append("', ")
                        .append("'").append("data:image/png;base64,$str").append("'")
                }
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

    @SuppressLint("StaticFieldLeak")
    inner class DownloadTask : AsyncTask<String, Void, File>() {

        /**
         * 主要是完成耗时的操作
         */
        override fun doInBackground(vararg url: String): File? {
            val imgUrl: URL?
            val bitmap: Bitmap?
            try {
                imgUrl = URL(url[0])
                val conn = imgUrl.openConnection()
                val http = conn as HttpURLConnection
                val length = http.contentLength
                conn.connect()
                //获得图像的字符流
                val `is` = conn.getInputStream()
                val bis = BufferedInputStream(`is`, length)
                bitmap = BitmapFactory.decodeStream(bis)
                bis.close()
                `is`.close()
                return save2Album(bitmap, "${System.currentTimeMillis()}.jpg")
            } catch (e: MalformedURLException) {
                println("[getNetWorkBitmap->]MalformedURLException")
                e.printStackTrace()
                Log.e("wtf", e.toString())
            } catch (e: IOException) {
                println("[getNetWorkBitmap->]IOException")
                e.printStackTrace()
                Log.e("wtf", e.toString())
            }
            return null
        }

        override fun onPostExecute(result: File) {
            super.onPostExecute(result)
            saveImage2Gallery(result)
        }
    }

    private fun save2Album(bitmap: Bitmap, fileName: String): File {
        val file = FileUtils.createFile(fileName, Environment.DIRECTORY_PICTURES)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
            runOnUiThread {
                Toast.makeText(
                    this@WebActivity,
                    getString(R.string.save_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(
                    this@WebActivity,
                    getString(R.string.save_fail),
                    Toast.LENGTH_SHORT
                ).show()
            }
            e.printStackTrace()
            Log.e("wtf", e.toString())
        } finally {
            try {
                fos!!.close()
            } catch (ignored: Exception) {
            }
        }
        return file
    }

    fun saveImage2Gallery(file: File) {
        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(
                Utils.context.contentResolver,
                file.absolutePath, file.name, null
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        // 最后通知图库更新
        val intent = Intent(
            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
            FileUtils.getImageContentUri(Utils.context, file)
        )
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        Utils.context.sendBroadcast(intent)
        if (file.exists()) {
            file.delete()
            FileUtils.updateFileFromDatabase(Utils.context, file)
        }
    }

    fun shareToFacebook(shareData: ShareData) {
        callbackManager = CallbackManager.Factory.create()
        shareDialog = ShareDialog(this)
        val linkContent = ShareLinkContent.Builder()
            .setContentUrl(Uri.parse(shareData.url))
            .setQuote(shareData.content)
            .build()
        shareDialog?.registerCallback(
            callbackManager,
            object : FacebookCallback<Sharer.Result> {
                override fun onSuccess(result: Sharer.Result?) {
                    shareCallBack(shareData.domainUrl, shareData.inviteCode, 1)
                }

                override fun onCancel() {
                }

                override fun onError(error: FacebookException?) {
                    Log.e("wtf", error.toString())
                }
            })
        shareDialog?.show(linkContent)
    }

    fun shareToWhatsApp(data: ShareData) {
        this.shareData = data
        shareWithPackageName("com.whatsapp", "", data.content)
    }

    private fun shareWithPackageName(
        packageName: String,
        className: String,
        content: String
    ) {
        if (AppInfo.isAPPInstalled(this, packageName)) {
            try {
                val vIt = Intent("android.intent.action.SEND")
                vIt.type = "text/plain"
                vIt.setPackage(packageName)
                if (!TextUtils.isEmpty(className)) {
                    vIt.setClassName(packageName, className)
                }
                vIt.putExtra(Intent.EXTRA_TEXT, content)
                vIt.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                startActivityForResult(vIt, SHARE_RESULT_CODE)
            } catch (ex: Exception) {
            }
        } else {
            ToastUtil.showToast(R.string.app_not_installed)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
                    val path = selectList[0].path
                    if (!TextUtils.isEmpty(path)) {
                        callback2Web(ImageUtil.compressImageToBase64(path)!!)
                    }
                }
                SHARE_RESULT_CODE -> {
                    shareCallBack(
                        shareData?.domainUrl ?: "",
                        shareData?.inviteCode ?: "",
                        2
                    )
                }
            }
        }
        if (requestCode == PAYTM_REQUEST_CODE && data != null) {
            Log.i("wtf", "PayTmCallback:" + data.getStringExtra("response"))
            ToastUtil.showToast("PayTmCallback:" + data.getStringExtra("response"))
        }
    }

    /**
     * 需要app 配合 调用接口 分享链接之后 调用接口
     * /user/userTask/dailyFaceAndWhats.do
     * inviteCode 邀请码
     * type  1:facebook  2 whatsApp
     */
    private fun shareCallBack(
        domainUrl: String,
        inviteCode: String,
        type: Int
    ) {
        runBlocking {
            val response = withContext(Dispatchers.IO) {
                val okHttpClient = OkHttpClient()
                okHttpClient.followRedirects
                val url =
                    "${domainUrl}/user/userTask/dailyFaceAndWhats.do?inviteCode=${inviteCode}&type=${type}"
                val request = Request.Builder().url(url).get().build()
                okHttpClient.newCall(request).execute()
            }
            if (response.code == 200) {
                webView.reload()
            }
        }
    }

    public fun openPayTm(data: String) {
        val payInfo: PayTmInfo = Gson().fromJson<PayTmInfo>(data, PayTmInfo::class.java)
        try {
            val bundle = Bundle()
            bundle.putDouble("nativeSdkForMerchantAmount", payInfo.amount)
            bundle.putString("orderid", payInfo.orderId)
            bundle.putString("txnToken", payInfo.textToken)
            bundle.putString("mid", payInfo.mid)
            val paytmIntent = Intent()
            paytmIntent.component =
                ComponentName("net.one97.paytm", "net.one97.paytm.AJRJarvisSplash")
            // You must have to pass hard coded 2 here, Else your transaction would not proceed.
            paytmIntent.putExtra("paymentmode", 2)
            paytmIntent.putExtra("bill", bundle)
            startActivityForResult(paytmIntent, PAYTM_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            runOnUiThread {
                val postData = StringBuilder()
                postData.append("MID=").append(payInfo.mid)
                    .append("&txnToken=").append(payInfo.textToken)
                    .append("&ORDER_ID=").append(payInfo.orderId);
                Launcher.with(this, WebActivity::class.java)
                    .putExtra(EX_TITLE, "PAY")
                    .putExtra(EX_REWRITE_TITLE, true)
                    .putExtra(EX_HAS_TITLE_BAR, true)
                    .putExtra(EX_POST_DATA, postData.toString())
                    .putExtra(
                        EX_URL,
                        URL_PAYTM_PRODUCTION + "?mid=" + payInfo.mid + "&orderId=" + payInfo.orderId
                    )
                    .putExtra(WebActivity.EX_TITLE_BG, titleBg ?: "")
                    .putExtra(WebActivity.EX_TITLE_FIELD_COLOR, titleFieldColor ?: "")
                    .execute()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Network.unregisterNetworkChangeReceiver(this, mNetworkChangeReceiver!!)
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
    open fun destroy(webView: WebView) {
        webView.stopLoading()
        (webView.parent as ViewGroup).removeView(webView)
        webView.removeAllViews()
        webView.destroy()
    }
}