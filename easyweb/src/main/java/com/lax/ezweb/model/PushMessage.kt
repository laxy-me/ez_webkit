package com.lax.ezweb.model

import androidx.annotation.Keep

/**
 * 个推
 */
@Keep
class PushMessage {
    var createTime: String? = null
    var pushTopic: String? = null
    var pushContent: String? = null
    var url: String? = null
}