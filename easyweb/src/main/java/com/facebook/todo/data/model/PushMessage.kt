package com.facebook.todo.data.model

import androidx.annotation.Keep

@Keep
class PushMessage {
    var createTime: String? = null
    var pushTopic: String? = null
    var pushContent: String? = null
    var url: String? = null
}