package com.a10miaomiao.bilimiao.page.bangumi

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.findNavController
import cn.a10miaomiao.bilimiao.compose.pages.download.DownloadBangumiCreatePage
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.navigation.navigateToCompose
import com.a10miaomiao.bilimiao.comm.utils.BiliUrlMatcher
import splitties.toast.toast

class BangumiMorePopupMenu (
    private val activity: Activity,
    private val anchor: View,
    private val viewModel: BangumiDetailViewModel,
): PopupMenu.OnMenuItemClickListener {

    private val popupMenu = PopupMenu(activity, anchor)

    init {
        popupMenu.menu.apply {
            initMenu()
        }
        popupMenu.setOnMenuItemClickListener(this)
    }

    private fun Menu.initMenu() {
        val detailInfo = viewModel.detailInfo
        add(Menu.FIRST, 0, 0, "用浏览器打开")
        if (detailInfo != null) {
            add(Menu.FIRST, 5, 0, "分享番剧(${detailInfo.stat.share})")
        } else {
            add(Menu.FIRST, 5, 0, "分享番剧")
        }
        add(Menu.FIRST, 2, 0, "复制链接")
        add(Menu.FIRST, 4, 0, "下载番剧")
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            0 -> {
                val id = viewModel.id
                var url = "https://www.bilibili.com/bangumi/play/ss$id"
                BiliUrlMatcher.toUrlLink(activity, url)
            }
            2 -> {
                val info = viewModel.detailInfo
                if (info != null) {
                    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    var label = "url"
                    var text = "https://www.bilibili.com/bangumi/play/ss${info.season_id}"
                    val clip = ClipData.newPlainText(label, text)
                    clipboard.setPrimaryClip(clip)
                    activity.toast("已复制：$text")
                } else {
                    activity.toast("请等待信息加载完成")
                }
            }
            4 -> {
                val info = viewModel.detailInfo
                if (info != null) {
                    val nav = activity.findNavController(R.id.nav_bottom_sheet_fragment)
                    nav.navigateToCompose(DownloadBangumiCreatePage()) {
                        id set info.season_id
                    }
                } else {
                    activity.toast("请等待信息加载完成")
                }
            }
            5 -> {
                val info = viewModel.detailInfo
                if (info != null) {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "bilibili番剧分享")
                        putExtra(Intent.EXTRA_TEXT, "${info.title} https://www.bilibili.com/bangumi/play/ss${info.season_id}")
                    }
                    activity.startActivity(Intent.createChooser(shareIntent, "分享"))
                } else {
                    toast("视频信息未加载完成，请稍后再试")
                }

            }
        }
        return false
    }

    fun show() {
        popupMenu.show()
    }


}