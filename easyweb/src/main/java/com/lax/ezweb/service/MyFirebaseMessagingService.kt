package com.lax.ezweb.service

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
import com.google.gson.Gson
import com.lax.ezweb.Preference
import com.lax.ezweb.R
import com.lax.ezweb.WebActivity
import com.lax.ezweb.data.model.PushMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        val TAG = MyFirebaseMessagingService::class.java.simpleName
        private const val PHONE_BRAND_SAMSUNG = "samsung"
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        Log.i(TAG, "onNewToken===$s")
        Preference.get().fcmToken = s
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            if (it) {
                if (/* Check if data needs to be processed by long running job */ false) {
                    // For long-running tasks (10 seconds or more) use WorkManager.
                    scheduleJob()
                } else {
                    // Handle message within 10 seconds
                    val pushMessage: PushMessage =
                        Gson().fromJson(remoteMessage.data["data"], PushMessage::class.java)
                    createNotification(baseContext, pushMessage)
                }
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(baseContext, it)
        }
    }

    private fun scheduleJob() {

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
        val builder = NotificationCompat.Builder(context, channelId)
        builder.setContentTitle(notificationTitle)
        builder.setContentText(notification.body)
        builder.setAutoCancel(true)
        builder.setWhen(notification.eventTime ?: System.currentTimeMillis())
        val brand = Build.BRAND
        builder.setSmallIcon(R.drawable.push)
        if (!TextUtils.isEmpty(brand) && brand.equals(PHONE_BRAND_SAMSUNG, ignoreCase = true)) {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.push)
            builder.setLargeIcon(bitmap)
        }
        builder.setAutoCancel(true);
        builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        val notificationManager: NotificationManager? =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //android 8.0 消息通知渠道
                val notificationChannel =
                    createNotificationChannel(channelId, notificationManager)
            }
            notificationManager.notify(notificationId, builder.build())
        }
    }

    private fun createNotification(context: Context, pushMessageModel: PushMessage) {
        val channelId = getString(R.string.app_name)
        val notificationTitle: String
        notificationTitle = if (!pushMessageModel.pushTopic.isNullOrBlank()) {
            pushMessageModel.pushTopic!!
        } else {
            channelId
        }
        val builder = NotificationCompat.Builder(context, channelId)
        builder.setContentTitle(notificationTitle)
        builder.setContentText(pushMessageModel.pushContent)
        builder.setAutoCancel(true)
        val createTime = try {
            pushMessageModel.createTime?.toLong() ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
        builder.setWhen(createTime)
        val brand = Build.BRAND
        val intent = setPendingIntent(context, pushMessageModel)
        builder.setSmallIcon(R.drawable.push)
        if (!TextUtils.isEmpty(brand) && brand.equals(PHONE_BRAND_SAMSUNG, ignoreCase = true)) {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.push)
            builder.setLargeIcon(bitmap)
        }
        builder.setContentIntent(intent)
        builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        val notificationManager: NotificationManager? =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //android 8.0 消息通知渠道
                val notificationChannel =
                    createNotificationChannel(channelId, notificationManager)
            }
            notificationManager.notify(notificationId, builder.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        channelId: String,
        notificationManager: NotificationManager
    ): NotificationChannel {
        val notificationChannel =
            NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT)
        notificationChannel.enableLights(true) //开启指示灯，如果设备有的话。
        notificationChannel.enableVibration(true) //开启震动
        notificationChannel.lightColor = Color.RED // 设置指示灯颜色
        notificationChannel.lockscreenVisibility =
            Notification.VISIBILITY_PRIVATE //设置是否应在锁定屏幕上显示此频道的通知
        notificationChannel.setShowBadge(true) //设置是否显示角标
        notificationChannel.setBypassDnd(true) // 设置绕过免打扰模式
        notificationChannel.vibrationPattern = longArrayOf(100, 200, 300, 400) //设置震动频率
        notificationChannel.description = channelId
        notificationManager.createNotificationChannel(notificationChannel)
        return notificationChannel
    }

    private fun setPendingIntent(context: Context, data: PushMessage): PendingIntent? {
        var intent: Intent?
        val url: String? = data.url
        if (TextUtils.isEmpty(url)) {
            val packageManager = context.packageManager
            intent = packageManager.getLaunchIntentForPackage(context.packageName)
        } else {
            intent = Intent(context, WebActivity::class.java)
            intent.putExtra(WebActivity.EX_URL, url)
            intent.putExtra(WebActivity.EX_HAS_TITLE_BAR, false)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }
}