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
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.igexin.sdk.GTIntentService
import com.igexin.sdk.PushConsts
import com.igexin.sdk.PushManager
import com.igexin.sdk.message.*
import com.lax.ezweb.Preference
import com.lax.ezweb.R
import com.lax.ezweb.WebActivity
import com.lax.ezweb.data.model.PushMessage


open class PushIntentService : GTIntentService() {
    override fun onReceiveMessageData(context: Context, msg: GTTransmitMessage) {
        val appid = msg.appid
        val taskid = msg.taskId
        val messageid = msg.messageId
        val payload = msg.payload
        val pkg = msg.pkgName
        val cid = msg.clientId
        // 第三方回执调用接口，actionid范围为90000-90999，可根据业务场景执行
        val result =
            PushManager.getInstance().sendFeedbackMessage(context, taskid, messageid, 90001)
        Log.d(
            TAG,
            "call sendFeedbackMessage = " + if (result) "success" else "failed"
        )
        Log.d(
            TAG,
            "onReceiveMessageData -> " + "appid = " + appid + "\ntaskid = " + taskid + "\nmessageid = " + messageid + "\npkg = " + pkg
                    + "\ncid = " + cid
        )
        if (payload == null) {
            Log.e(TAG, "receiver payload = null")
        } else {
            val data = String(payload)
            Log.d(TAG, "receiver payload = $data")
            handleMessage(context, data)
        }
    }

    private fun handleMessage(context: Context, data: String) {
        Log.d(TAG, "===data  $data")
        try {
            val pushMessage: PushMessage = Gson().fromJson(data, PushMessage::class.java)
            createNotification(context, pushMessage)
        } catch (e: JsonSyntaxException) {
            Log.d(TAG, "handleMessage: $e")
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
        builder.setWhen(System.currentTimeMillis())
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

    private fun setPendingIntent(context: Context, data: PushMessage): PendingIntent {
        var intent = Intent(context, WebActivity::class.java)
        val url: String? = data.url
        if (TextUtils.isEmpty(url)) {
            val packageManager = context.packageManager
            intent = packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(
                context,
                WebActivity::class.java
            )
        } else {
            intent.putExtra(WebActivity.EX_URL, url)
            intent.putExtra(WebActivity.EX_HAS_TITLE_BAR, false)
        }
        if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    private fun setTagResult(setTagCmdMsg: SetTagCmdMessage) {
        val sn = setTagCmdMsg.sn
        val code = setTagCmdMsg.code
        var text = "设置标签失败, 未知异常"
        when (code.toInt()) {
            PushConsts.SETTAG_SUCCESS -> text = "设置标签成功"
            PushConsts.SETTAG_ERROR_COUNT -> text = "设置标签失败, tag数量过大, 最大不能超过200个"
            PushConsts.SETTAG_ERROR_FREQUENCY -> text = "设置标签失败, 频率过快, 两次间隔应大于1s且一天只能成功调用一次"
            PushConsts.SETTAG_ERROR_REPEAT -> text = "设置标签失败, 标签重复"
            PushConsts.SETTAG_ERROR_UNBIND -> text = "设置标签失败, 服务未初始化成功"
            PushConsts.SETTAG_ERROR_EXCEPTION -> text = "设置标签失败, 未知异常"
            PushConsts.SETTAG_ERROR_NULL -> text = "设置标签失败, tag 为空"
            PushConsts.SETTAG_NOTONLINE -> text = "还未登陆成功"
            PushConsts.SETTAG_IN_BLACKLIST -> text = "该应用已经在黑名单中,请联系售后支持!"
            PushConsts.SETTAG_NUM_EXCEED -> text = "已存 tag 超过限制"
            else -> {
            }
        }
        Log.d(
            TAG,
            "settag result sn = $sn, code = $code, text = $text"
        )
    }

    override fun onReceiveServicePid(
        context: Context,
        pid: Int
    ) {
        Log.d(TAG, "onReceiveServicePid -> $pid")
    }

    override fun onReceiveClientId(context: Context, clientid: String) {
        Log.e(TAG, "onReceiveClientId -> clientid = $clientid")
        Preference.get().pushId = clientid
    }

    override fun onReceiveOnlineState(context: Context, online: Boolean) {
        Log.d(TAG, "onReceiveOnlineState -> " + if (online) "online" else "offline")
    }

    override fun onReceiveCommandResult(context: Context, cmdMessage: GTCmdMessage) {
        Log.d(TAG, "onReceiveCommandResult -> $cmdMessage")
        when (cmdMessage.action) {
            PushConsts.SET_TAG_RESULT -> {
                setTagResult(cmdMessage as SetTagCmdMessage)
            }
            PushConsts.BIND_ALIAS_RESULT -> {
                bindAliasResult(cmdMessage as BindAliasCmdMessage)
            }
            PushConsts.UNBIND_ALIAS_RESULT -> {
                unbindAliasResult(cmdMessage as UnBindAliasCmdMessage)
            }
            PushConsts.THIRDPART_FEEDBACK -> {
                feedbackResult(cmdMessage as FeedbackCmdMessage)
            }
        }
    }

    override fun onNotificationMessageArrived(context: Context, message: GTNotificationMessage) {
        Log.d(
            TAG,
            "onNotificationMessageArrived -> " + "appid = " + message.appid + "\ntaskid = " + message.taskId + "\nmessageid = "
                    + message.messageId + "\npkg = " + message.pkgName + "\ncid = " + message.clientId + "\ntitle = "
                    + message.title + "\ncontent = " + message.content
        )
    }

    override fun onNotificationMessageClicked(context: Context, message: GTNotificationMessage) {
        Log.d(
            TAG,
            "onNotificationMessageClicked -> " + "appid = " + message.appid + "\ntaskid = " + message.taskId + "\nmessageid = "
                    + message.messageId + "\npkg = " + message.pkgName + "\ncid = " + message.clientId + "\ntitle = "
                    + message.title + "\ncontent = " + message.content
        )
    }

    private fun bindAliasResult(bindAliasCmdMessage: BindAliasCmdMessage) {
        val sn = bindAliasCmdMessage.sn
        val code = bindAliasCmdMessage.code
        var text: Int = R.string.bind_alias_unknown_exception
        when (Integer.valueOf(code)) {
            PushConsts.BIND_ALIAS_SUCCESS -> text = R.string.bind_alias_success
            PushConsts.ALIAS_ERROR_FREQUENCY -> text = R.string.bind_alias_error_frequency
            PushConsts.ALIAS_OPERATE_PARAM_ERROR -> text = R.string.bind_alias_error_param_error
            PushConsts.ALIAS_REQUEST_FILTER -> text = R.string.bind_alias_error_request_filter
            PushConsts.ALIAS_OPERATE_ALIAS_FAILED -> text = R.string.bind_alias_unknown_exception
            PushConsts.ALIAS_CID_LOST -> text = R.string.bind_alias_error_cid_lost
            PushConsts.ALIAS_CONNECT_LOST -> text = R.string.bind_alias_error_connect_lost
            PushConsts.ALIAS_INVALID -> text = R.string.bind_alias_error_alias_invalid
            PushConsts.ALIAS_SN_INVALID -> text = R.string.bind_alias_error_sn_invalid
            else -> {
            }
        }
        Log.d(
            TAG,
            "bindAlias result sn = $sn, code = $code, text = " + resources.getString(
                text
            )
        )
    }

    private fun unbindAliasResult(unBindAliasCmdMessage: UnBindAliasCmdMessage) {
        val sn = unBindAliasCmdMessage.sn
        val code = unBindAliasCmdMessage.code
        var text: Int = R.string.unbind_alias_unknown_exception
        when (code.toInt()) {
            PushConsts.UNBIND_ALIAS_SUCCESS -> text = R.string.unbind_alias_success
            PushConsts.ALIAS_ERROR_FREQUENCY -> text = R.string.unbind_alias_error_frequency
            PushConsts.ALIAS_OPERATE_PARAM_ERROR -> text = R.string.unbind_alias_error_param_error
            PushConsts.ALIAS_REQUEST_FILTER -> text = R.string.unbind_alias_error_request_filter
            PushConsts.ALIAS_OPERATE_ALIAS_FAILED -> text = R.string.unbind_alias_unknown_exception
            PushConsts.ALIAS_CID_LOST -> text = R.string.unbind_alias_error_cid_lost
            PushConsts.ALIAS_CONNECT_LOST -> text = R.string.unbind_alias_error_connect_lost
            PushConsts.ALIAS_INVALID -> text = R.string.unbind_alias_error_alias_invalid
            PushConsts.ALIAS_SN_INVALID -> text = R.string.unbind_alias_error_sn_invalid
            else -> {
            }
        }
        Log.d(
            TAG,
            "unbindAlias result sn = $sn, code = $code, text = " + resources.getString(
                text
            )
        )
    }

    private fun feedbackResult(feedbackCmdMsg: FeedbackCmdMessage) {
        val appid = feedbackCmdMsg.appid
        val taskid = feedbackCmdMsg.taskId
        val actionid = feedbackCmdMsg.actionId
        val result = feedbackCmdMsg.result
        val timestamp = feedbackCmdMsg.timeStamp
        val cid = feedbackCmdMsg.clientId
        Log.d(
            TAG,
            "onReceiveCommandResult -> " + "appid = " + appid + "\ntaskid = " + taskid + "\nactionid = " + actionid + "\nresult = " + result
                    + "\ncid = " + cid + "\ntimestamp = " + timestamp
        )
    }

    companion object {
        private const val TAG = "PushIntentService"
        private const val PHONE_BRAND_SAMSUNG = "samsung"
    }
}