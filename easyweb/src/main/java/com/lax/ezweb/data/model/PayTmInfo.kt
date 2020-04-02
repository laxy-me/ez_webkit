package com.lax.ezweb.data.model

import androidx.annotation.Keep

/**
 *
 * @author yangguangda
 * @date 2020/3/20
 */
@Keep
class PayTmInfo {
    var textToken: String = "";
    var orderId: String = "";
    var mid: String = "";
    var amount: Double = 0.0
}