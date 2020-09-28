package com.facebook.todo.data.model

import androidx.annotation.Keep

@Keep
class PayTmInfo {
    var textToken: String = "";
    var orderId: String = "";
    var mid: String = "";
    var amount: Double = 0.0
}