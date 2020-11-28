package com.lax.example

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.eztd.arm.ExtraKeys
import com.eztd.arm.base.BaseActivity
import com.eztd.arm.tools.AppInfo
import com.eztd.arm.web.Web
import com.eztd.arm.web.WebActivity
import com.google.gson.Gson
import com.lax.example.model.VestConfig
import com.lax.example.model.VestModel
import io.branch.referral.Branch
import io.branch.referral.BranchError
import kotlinx.coroutines.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext


class SplashActivity : BaseActivity(), CoroutineScope {
    companion object {
        const val REQUEST_SETTINGS = 12345
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private var job: Job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        launch {
//            PDataStore.getInstance(applicationContext).setData<String>("aaa", "hahahaha")
//            var set = HashSet<String>()
//            set.add("aa1")
//            set.add("aa2")
//            PDataStore.getInstance(applicationContext).setSetData("aab", set)
//            val data = PDataStore.getInstance(applicationContext).getData<String>("aaa", "0")
//            val data1 =
//                PDataStore.getInstance(applicationContext).getSetData("aab", HashSet<String>())
//            Log.e("wtf", data)
//            Log.e("wtf", data1.toString())
//        }
//
//        adid.text = Preference.get().Gaid
//        androidId.text = AppInfo.getAndroidId(applicationContext)
        val pushMessage = JSONObject("{\"createTime\":1606305277895,\"pushTopic\":\"topic\",\"pushContent\":\"正式链接，可以测试谷歌登录，上传图片\",\"url\":\"https:\\/\\/eto.master365pro.com\\/\"}")
        createNotification(baseContext, pushMessage)
        Launcher.with(this, Web::class.java)
            .putExtra(ExtraKeys.EX_TITLE, "")
            .putExtra(ExtraKeys.EX_HAS_TITLE_BAR, false)
//            .putExtra(ExtraKeys.EX_URL, "https://c1.mufg365.com/app_bridge.html")
//            .putExtra(WebActivity.EX_URL, "https://ga1.master365pro.com/en/trade/index")
//            .putExtra(WebActivity.EX_URL, "https://op.optionind.com/trade/index")
//            .putExtra(ExtraKeys.EX_URL, "https://quasi.citicoption.com/")
//            .putExtra(ExtraKeys.EX_URL, "https://date.master365pro.com")
            .putExtra(ExtraKeys.EX_URL, "https://eto.master365pro.com/")
//            .putExtra(ExtraKeys.EX_URL, "http://oph55.dzz.cc/trade/index")
//            .putExtra(ExtraKeys.EX_URL, "https://eto.master365pro.com/")
//            .putExtra(WebActivity.EX_URL, "http://rummyind.com/paytm/seamless-basic.php")
//            .putExtra(WebActivity.EX_URL, "https://wayangpay.co.id/payer/en-home-index.html?notifyUrl=https://payback.citicoption.com/user/deposit/notify/wayangpay.do&amount=200000.0&callbackUrl=https://quasi.citicoption.com/users&goodsInfo=Options&mchId=010564&outTradeNo=OP1598940811503152&sign=6a8f250bb0bfdd8d632bcfc0ebce2ebd")
//            .putExtra(WebActivity.EX_URL, "https://ft.master365pro.com")
//            .putExtra(WebActivity.EX_URL, "http://oph52.dzz.cc/trade/index")
            .execute()
        finish()

//        Launcher.with(this, PureWebActivity::class.java)
//            .putExtra(PureWebActivity.EX_TITLE, "")
//            .putExtra(PureWebActivity.EX_HAS_TITLE_BAR, false)
//            .putExtra(WebActivity.EX_URL, "https://c1.mufg365.com/app_bridge.html")
//            .putExtra(WebActivity.EX_URL, "https://ga1.master365pro.com/en/trade/index")
//            .putExtra(WebActivity.EX_URL, "https://op.optionind.com/trade/index")
//            .putExtra(PureWebActivity.EX_URL, "https://payments.cashfree.com/order/#12vss9n73pesw041dzvdi")
//            .putExtra(WebActivity.EX_URL, "https://wayangpay.co.id/payer/en-home-index.html?notifyUrl=https://payback.citicoption.com/user/deposit/notify/wayangpay.do&amount=200000.0&callbackUrl=https://quasi.citicoption.com/users&goodsInfo=Options&mchId=010564&outTradeNo=OP1598940811503152&sign=6a8f250bb0bfdd8d632bcfc0ebce2ebd")
//            .putExtra(WebActivity.EX_URL, "https://ft.master365pro.com")
//            .putExtra(WebActivity.EX_URL, "http://oph52.dzz.cc/trade/index")
//            .execute()
//        finish()
//        init()
//        AppInfo.getAndroidId(this)
    }

    private fun createNotification(context: Context, pushMessageModel: JSONObject) {
        val channelId = getString(com.eztd.arm.R.string.app_name)
        val notificationTitle: String
        notificationTitle = if (!pushMessageModel.optString("pushTopic").isNullOrBlank()) {
            pushMessageModel.optString("pushTopic")
        } else {
            channelId
        }
        val builder = NotificationCompat.Builder(context, channelId).apply {
            setContentTitle(notificationTitle)
            setContentText(pushMessageModel.optString("pushContent"))
            setAutoCancel(true)
            val createTime = try {
                pushMessageModel.optString("createTime").toLong()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
            setWhen(createTime)
            val brand = Build.BRAND
            val intent = setPendingIntent(context, pushMessageModel)
            setSmallIcon(com.eztd.arm.R.mipmap.push)
            if (!TextUtils.isEmpty(brand) && brand.equals("samsung", ignoreCase = true)) {
                val bitmap = BitmapFactory.decodeResource(context.resources, com.eztd.arm.R.mipmap.push)
                setLargeIcon(bitmap)
            }
            setContentIntent(intent)
            setDefaults(NotificationCompat.DEFAULT_ALL)
        }
        val notificationManager: NotificationManager? =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager?.apply {
            val notificationId = System.currentTimeMillis().toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(channelId, this)
            }
            this.notify(notificationId, builder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        channelId: String,
        notificationManager: NotificationManager
    ): NotificationChannel {
        val notificationChannel =
            NotificationChannel(
                channelId,
                channelId,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(true) //开启指示灯，如果设备有的话。
                enableVibration(true) //开启震动
                lightColor = Color.RED // 设置指示灯颜色
                lockscreenVisibility =
                    Notification.VISIBILITY_PRIVATE //设置是否应在锁定屏幕上显示此频道的通知
                setShowBadge(true) //设置是否显示角标
                setBypassDnd(true) // 设置绕过免打扰模式
                vibrationPattern = longArrayOf(100, 200, 300, 400) //设置震动频率
                description = channelId
            }
        notificationManager.createNotificationChannel(notificationChannel)
        return notificationChannel
    }

    private fun setPendingIntent(context: Context, data: JSONObject): PendingIntent? {
        val intent: Intent?
        val url: String? = data.optString("url")
        if (TextUtils.isEmpty(url)) {
            val packageManager = context.packageManager
            intent = packageManager.getLaunchIntentForPackage(context.packageName)
        } else {
            intent = Intent(context, WebActivity::class.java).apply {
                putExtra("data", TestData(url))
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
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
            AppInfo.getMetaData(applicationContext, "CHANNEL")
        )
        newBuilder.addQueryParameter("deviceId", AppInfo.getDeviceHardwareId(this))
        newBuilder.addQueryParameter("timestamp", System.currentTimeMillis().toString())
        val request = Request.Builder().url(newBuilder.build()).get().build()
        val response = OkHttpClient().newCall(request).execute()
        return Gson().fromJson(response.body?.string() ?: "", VestModel::class.java)
    }

    private fun openMain(data: VestConfig?) {
        val launcher = Launcher.with(this, Web::class.java)
            .putExtra(ExtraKeys.EX_TITLE, "")
            .putExtra(ExtraKeys.EX_HAS_TITLE_BAR, false)
            .putExtra(ExtraKeys.EX_URL, data?.h5Url ?: "")
            .putExtra(ExtraKeys.EX_TITLE_BG, data?.backgroundCol ?: "")
            .putExtra(ExtraKeys.EX_TITLE_FIELD_COLOR, data?.fieldCol ?: "")
        if (data?.advOn == 1) {
            launcher
                .putExtra(
                    ExtraKeys.EX_AD_URL,
                    data.advImg
                        ?: ""
                )
                .putExtra(ExtraKeys.EX_AD_CONTENT, data.advUrl ?: "")
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

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}
