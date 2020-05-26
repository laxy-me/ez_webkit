package com.lax.ezweb

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference
import java.util.*


/**
 *
 * @author yangguangda
 * @date 2020/5/26
 */
open class MyWebChromeClient : WebChromeClient() {
    //定义接受返回值
    private var uploadFile: ValueCallback<Uri>? = null
    private var uploadFiles: ValueCallback<Array<Uri>>? = null
    private val CHOOSE_REQUEST_CODE = 0x9001
    private var mActivity: WeakReference<Activity>? = null
    fun setActivity(mActivity: Activity) {
        this.mActivity = WeakReference(mActivity)
    }

    /**
     * Android < 3.0
     *
     * @param valueCallback
     */
    open fun openFileChooser(valueCallback: ValueCallback<Uri?>) {
        commonRefect(
            this, "openFileChooser", arrayOf(valueCallback),
            ValueCallback::class.java
        )
    }

    /**
     * Android  >= 3.0
     *
     * @param valueCallback
     * @param acceptType
     */
    open fun openFileChooser(
        valueCallback: ValueCallback<*>,
        acceptType: String
    ) {
        commonRefect(
            this, "openFileChooser", arrayOf(valueCallback, acceptType),
            ValueCallback::class.java,
            String::class.java
        )
    }

    /**
     * Android  >= 4.1
     *
     * @param uploadFile
     * @param acceptType
     * @param capture
     */
    open fun openFileChooser(
        uploadFile: ValueCallback<Uri?>,
        acceptType: String,
        capture: String
    ) {
        commonRefect(
            this, "openFileChooser", arrayOf(uploadFile, acceptType, capture),
            ValueCallback::class.java,
            String::class.java,
            String::class.java
        )
    }

    open fun commonRefect(
        o: WebChromeClient?,
        mothed: String,
        os: Array<Any>,
        vararg clazzs: Class<*>
    ) {
        try {
            if (o == null) {
                return
            }
            val clazz: Class<*> = o.javaClass
            val mMethod = clazz.getMethod(mothed, *clazzs)
            mMethod.invoke(o, *os)
        } catch (ignore: Exception) {
            ignore.printStackTrace()
        }
    }

    // For Android  >= 5.0
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        this.uploadFiles = filePathCallback
        openFileChooseProcess(webView, filePathCallback, fileChooserParams)
        return true
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    open fun openFileChooseProcess(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ) {
        Log.i(
            "MyWebChromeClient",
            "fileChooserParams:" + fileChooserParams?.acceptTypes.toString()
                    + "  getTitle:" + fileChooserParams?.title.toString()
                    + " accept:" + Arrays.toString(fileChooserParams?.acceptTypes).toString()
                    + " length:" + fileChooserParams?.acceptTypes?.size.toString()
                    + "  :"
                    + fileChooserParams?.isCaptureEnabled.toString()
                    + "  "
                    + fileChooserParams?.filenameHint.toString()
                    + "  intent:" + fileChooserParams?.createIntent().toString()
                    + "   mode:" + fileChooserParams?.mode
        )
        val mIntent: Intent? = fileChooserParams?.createIntent()
        if (mIntent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mIntent.action == Intent.ACTION_GET_CONTENT) {
                mIntent.action = Intent.ACTION_OPEN_DOCUMENT
            }
            val activity = mActivity?.get()
            activity?.startActivityForResult(
                Intent.createChooser(mIntent, "Choose"),
                CHOOSE_REQUEST_CODE
            )
        }
        val i = Intent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            i.action = Intent.ACTION_OPEN_DOCUMENT
        } else {
            i.action = Intent.ACTION_GET_CONTENT
        }
        i.addCategory(Intent.CATEGORY_OPENABLE)
//        if (TextUtils.isEmpty(this.mAcceptType)) {
        i.type = "*/*"
//        } else {
//            i.type = this.mAcceptType
//        }
        val activity = mActivity?.get()
        activity?.startActivityForResult(Intent.createChooser(i, "Choose"), CHOOSE_REQUEST_CODE)
    }

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("requestCode===", "$requestCode====")
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CHOOSE_REQUEST_CODE -> {
                    if (null != uploadFile) {
                        val result =
                            if (data == null || resultCode != Activity.RESULT_OK) null else data.data
                        uploadFile!!.onReceiveValue(result)
                        uploadFile = null
                    }
                    if (null != uploadFiles) {
                        val result =
                            if (data == null || resultCode != Activity.RESULT_OK) null else data.data
                        uploadFiles!!.onReceiveValue(arrayOf<Uri>(result!!))
                        uploadFiles = null
                    }
                }
                else -> {
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            if (null != uploadFile) {
                uploadFile!!.onReceiveValue(null)
                uploadFile = null
            }
            if (null != uploadFiles) {
                uploadFiles!!.onReceiveValue(null)
                uploadFiles = null
            }
        }
    }
}