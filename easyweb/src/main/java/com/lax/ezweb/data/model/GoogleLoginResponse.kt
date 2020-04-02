package com.lax.ezweb.data.model

import androidx.annotation.Keep

/**
 *
 * @author yangguangda
 * @date 2020/3/25
 */
@Keep
class GoogleLoginResponse {
    var code: Int = 0
    var data: GoogleLoginToken? = null
}