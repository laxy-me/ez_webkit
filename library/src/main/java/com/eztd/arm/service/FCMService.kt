package com.eztd.arm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.eztd.arm.R
import com.eztd.arm.tools.Preference
import com.eztd.arm.web.WebActivity
import org.json.JSONObject

class FCMService : FirebaseMessagingService() {
    var tag: String = FCMService::class.java.simpleName
    override fun onNewToken(s: String) {
        super.onNewToken(s)
        Log.i(tag, "onNewToken===$s")
        Preference.get().fcmToken = s
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(tag, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d(tag, "Message data payload: " + remoteMessage.data)
            if (it) {
                val pushMessage = JSONObject(remoteMessage.data["data"] ?: "")
                createNotification(baseContext, pushMessage)
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(tag, "Message Notification Body: ${it.body}")
            showNotification(baseContext, it)
        }
    }

    private fun showNotification(context: Context, notification: RemoteMessage.Notification) {
        val channelId = getString(R.string.app_name)
        val notificationTitle: String
        val title = notification.title
        notificationTitle = if (!title.isNullOrBlank()) {
            title
        } else {
            channelId
        }
        val builder = NotificationCompat.Builder(context, channelId).apply {
            setContentTitle(notificationTitle)
            setContentText(notification.body)
            setAutoCancel(true)
            setWhen(notification.eventTime ?: System.currentTimeMillis())
            val brand = Build.BRAND
            setSmallIcon(R.mipmap.push)
            if (!TextUtils.isEmpty(brand) && brand.equals("samsung", ignoreCase = true)) {
                val bitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.push)
                setLargeIcon(bitmap)
            }
            setAutoCancel(true)
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

    private fun createNotification(context: Context, pushMessageModel: JSONObject) {
        val channelId = getString(R.string.app_name)
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
            setSmallIcon(R.mipmap.push)
            if (!TextUtils.isEmpty(brand) && brand.equals("samsung", ignoreCase = true)) {
                val bitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.push)
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
                putExtra(com.eztd.arm.ExtraKeys.EX_URL, url)
                putExtra(com.eztd.arm.ExtraKeys.EX_HAS_TITLE_BAR, false)
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
}