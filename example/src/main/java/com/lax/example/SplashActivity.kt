package com.lax.example

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.lax.ezweb.Launcher
import com.lax.ezweb.WebActivity
import com.lax.ezweb.base.BaseActivity
import com.lax.ezweb.data.model.VestConfig
import com.lax.ezweb.data.model.VestModel
import com.lax.ezweb.tools.AppInfo
import io.branch.referral.Branch
import io.branch.referral.BranchError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class SplashActivity : BaseActivity() {
    companion object {
        const val REQUEST_SETTINGS = 12345
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Launcher.with(this, WebActivity::class.java)
            .putExtra(WebActivity.EX_TITLE, "")
            .putExtra(WebActivity.EX_HAS_TITLE_BAR, false)
            .putExtra(WebActivity.EX_URL, "https://app.goex24.com/?sign=firecoin")
            .execute()
//        init()
    }

    override fun showRationaleDialog(permissions: Array<String>, ration: IntArray) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.need_permissions))
            .setCancelable(false)
            .setMessage(permissionDesc(permissions))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                init()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.textColorAssist))
    }

    override fun showRejectDialog(permissions: Array<String>, reject: IntArray) {
        //拒绝授权
        Log.i(TAG, "onPermissionsDenied:$permissions")
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permissions in app settings.
        val sb = permissionDesc(permissions)
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.need_permissions))
            .setCancelable(false)
            .setMessage(sb)
            .setPositiveButton(getString(R.string.go_to_settings)) { dialog, _ ->
                startActivityForResult(getAppDetailSettingIntent(), REQUEST_SETTINGS)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
            .setTextColor(ContextCompat.getColor(this, R.color.textColorAssist))
    }

    private fun getAppDetailSettingIntent(): Intent {
        val localIntent = Intent()
        localIntent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        localIntent.data = Uri.fromParts("package", packageName, null)
        return localIntent
    }

    private fun init() {
        requestPermission(
            arrayOf(Manifest.permission.READ_PHONE_STATE),
            intArrayOf(),
            object : AfterPermissionGranted {
                override fun permissionGranted() {
                    request()
                }
            })
    }

    private fun request() {
        runBlocking {
            val result = withContext(Dispatchers.IO) {
                getConfig()
            }
            openMain(result?.data)
        }
    }

    private fun getConfig(): VestModel? {
        val newBuilder =
            "${BuildConfig.HOST}/admin/client/vestSign.do".toHttpUrlOrNull()?.newBuilder()!!
        newBuilder.addQueryParameter("vestCode", BuildConfig.VEST_CODE)
        newBuilder.addQueryParameter("version", BuildConfig.VERSION_NAME)
        newBuilder.addQueryParameter(
            "channelCode",
            AppInfo.getMetaData(applicationContext, AppInfo.Meta.CHANNEL)
        )
        newBuilder.addQueryParameter("deviceId", AppInfo.getDeviceHardwareId(this))
        newBuilder.addQueryParameter("timestamp", System.currentTimeMillis().toString())
        val request = Request.Builder().url(newBuilder.build()).get().build()
        val response = OkHttpClient().newCall(request).execute()
        return Gson().fromJson(response.body?.string() ?: "", VestModel::class.java)
    }

    private fun openMain(data: VestConfig?) {
        val launcher = Launcher.with(this, WebActivity::class.java)
            .putExtra(WebActivity.EX_TITLE, "")
            .putExtra(WebActivity.EX_HAS_TITLE_BAR, false)
            .putExtra(WebActivity.EX_URL, "http://47.104.65.60:443/topup")
            .putExtra(WebActivity.EX_TITLE_BG, data?.backgroundCol ?: "")
            .putExtra(WebActivity.EX_TITLE_FIELD_COLOR, data?.fieldCol ?: "")
        if (data?.advOn == 1) {
            launcher
                .putExtra(
                    WebActivity.EX_AD_URL,
                    data.advImg
                        ?: "https://dss0.bdstatic.com/70cFvHSh_Q1YnxGkpoWK1HF6hhy/it/u=3386247472,87720242&fm=26&gp=0.jpg"
                )
                .putExtra(WebActivity.EX_AD_CONTENT, data.advUrl ?: "")
        }
        launcher.execute()
        finish()
    }

    override fun onStart() {
        super.onStart()
        Branch.sessionBuilder(this).withCallback(BranchListener).withData(intent.data).init()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        Branch.sessionBuilder(this).withCallback(BranchListener).withData(intent.data).reInit()
    }

    object BranchListener : Branch.BranchReferralInitListener {
        override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
            if (error == null) {
                Log.i("BRANCH SDK", referringParams.toString())
            } else {
                Log.e("BRANCH SDK", error.message)
            }
        }
    }
}
