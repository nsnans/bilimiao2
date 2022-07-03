package com.a10miaomiao.bilimiao.comm.delegate.player.model

import bilibili.app.playurl.v1.PlayURLGrpc
import bilibili.app.playurl.v1.Playurl
import com.a10miaomiao.bilimiao.comm.apis.PlayerAPI
import com.a10miaomiao.bilimiao.comm.network.ApiHelper
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp
import com.a10miaomiao.bilimiao.comm.network.request
import com.a10miaomiao.bilimiao.comm.utils.CompressionTools
import com.a10miaomiao.bilimiao.comm.utils.DebugMiao
import com.a10miaomiao.bilimiao.widget.player.BiliDanmukuParser
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import java.io.ByteArrayInputStream
import java.io.InputStream

class VideoPlayerSource(
    override val title: String,
    override val coverUrl: String,
    var aid: String,
    var cid: String,
): BasePlayerSource {

    override suspend fun getPlayerUrl(quality: Int): String {
//        val req = Playurl.PlayURLReq.newBuilder()
//            .setAid(aid.toLong())
//            .setCid(cid.toLong())
//            .setQn(quality.toLong())
//            .setDownload(0)
//            .setForceHost(2)
//            .setFourk(false)
//            .build()
//        val result = PlayURLGrpc.getPlayURLMethod()
//            .request(req)
//            .awaitCall()
//        return getMpdUrl(quality, result.dash)
        val res = BiliApiService.playerAPI
            .getVideoPalyUrl(aid, cid, quality, dash = true)
        val dash = res.dash
        if (dash != null) {
            return DashSource(quality, dash).getMDPUrl()
        }
        val durl = res.durl
        return durl!![0].url
    }

    override suspend fun getDanmakuParser(): BaseDanmakuParser? {
        val inputStream = getBiliDanmukuStream()
        return if (inputStream == null) {
            null
        } else {
            val loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI)
            loader.load(inputStream)
            val parser = BiliDanmukuParser()
            val dataSource = loader.dataSource
            parser.load(dataSource)
            parser
        }
    }

    private suspend fun getBiliDanmukuStream(): InputStream? {
        val res = BiliApiService.playerAPI.getDanmakuList(cid)
            .awaitCall()
        val body = res.body()
        return if (body == null) {
            null
        } else {
            ByteArrayInputStream(CompressionTools.decompressXML(body.bytes()))
        }
    }

    override suspend fun historyReport(progress: Long) {
        try {
            val realtimeProgress = progress.toString()  // 秒数
            MiaoHttp.request {
                url = "https://api.bilibili.com/x/v2/history/report"
                formBody = ApiHelper.createParams(
                    "aid" to aid,
                    "cid" to cid,
                    "progress" to realtimeProgress,
                    "realtime" to realtimeProgress,
                    "type" to "3"
                )
                method = MiaoHttp.POST
            }.awaitCall()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}