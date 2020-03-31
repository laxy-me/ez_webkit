package com.lax.ezweb.model

import androidx.annotation.Keep

/**
 * @author yangguangda
 * @date 2020-01-08
 */
@Keep
class VestConfig(
    var advImg: String? = "",
    var advUrl: String? = "",
    var advOn: Int,
    var gtKey: String,
    var gtId: String,
    var gtSecert: String,
    var channelName: String,
    var version: String,
    var vestCode: String,
    var vestName: String,
    //业务链接
    var h5Url: String,
    //马甲链接
    var vestUrl: String,
    var backgroundCol: String,
    var fieldCol: String,
    var status: Int,
    var channelCode: String
)