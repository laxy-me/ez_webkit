package com.lax.example.service

import com.google.firebase.messaging.FirebaseMessagingService

/**
 *
 * @author yangguangda
 * @date 2020/4/7
 */
class FMSService : FirebaseMessagingService() {
    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
    }
}