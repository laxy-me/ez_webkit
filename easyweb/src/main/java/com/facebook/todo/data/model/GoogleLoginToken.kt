package com.facebook.todo.data.model

import androidx.annotation.Keep

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