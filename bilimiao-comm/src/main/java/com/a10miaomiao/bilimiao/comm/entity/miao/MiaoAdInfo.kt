package com.a10miaomiao.bilimiao.comm.entity.miao

import kotlinx.serialization.Serializable

/**
 * 视频推荐（广告）
 */
@Serializable
data class MiaoAdInfo (
    var code: Int,
    var msg: String,
    var data: DataBean
){
    data class DataBean(
        var ad: AdBean,
        var version: VersionBean,
        var settingList: List<MiaoSettingInfo>
    )
    data class AdBean(
        var isShow: Boolean,
        var title: String,
        var link: LinkBean
    )
    data class LinkBean(
        var text: String,
        var url: String
    )
    data class VersionBean(
        var versionCode: Long,
        var versionName: String,
        var miniVersionCode: Long, //最小版本号，小于此版本必须更新
        var content: String,
        var url: String
    )
}