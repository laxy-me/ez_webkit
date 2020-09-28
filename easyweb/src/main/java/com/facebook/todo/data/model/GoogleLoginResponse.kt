package com.facebook.todo.data.model

import androidx.annotation.Keep

@Keep
class GoogleLoginResponse {
    var code: Int = 0
    var data: GoogleLoginToken? = null
}