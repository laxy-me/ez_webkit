package com.lax.ezweb.model

import androidx.annotation.Keep

/**
 * @author yangguangda
 * @date 2020-01-17
 */
@Keep
class GoogleLoginToken {
    /**
     * token1 : anN4aG9leHpheWlndWJuY3RocG9udHFoa2tjMg==
     * token2 : NTkxMjRkM2ZiYmFhMmMwNjYyZjg0MDI0NjNjYzMyMWU=
     * url : https://bb.skr.today/zh_cn/?sign=11
     */
    var token1: String = ""
    var token2: String = ""
    var url: String? = null

}