package cn.a10miaomiao.bilimiao.compose.pages.user

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.pager.PagerState
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import cn.a10miaomiao.bilimiao.compose.base.navigate
import cn.a10miaomiao.bilimiao.compose.comm.defaultNavOptions
import cn.a10miaomiao.bilimiao.compose.comm.navigation.findComposeNavController
import cn.a10miaomiao.bilimiao.compose.pages.bangumi.BangumiDetailPage
import com.a10miaomiao.bilimiao.comm.apis.UserApi
import com.a10miaomiao.bilimiao.comm.entity.MessageInfo
import com.a10miaomiao.bilimiao.comm.entity.ResultInfo
import com.a10miaomiao.bilimiao.comm.entity.user.SpaceInfo
import com.a10miaomiao.bilimiao.comm.mypage.MyPage
import com.a10miaomiao.bilimiao.comm.network.BiliApiService
import com.a10miaomiao.bilimiao.comm.network.MiaoHttp.Companion.gson
import com.a10miaomiao.bilimiao.comm.store.FilterStore
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import com.kongzue.dialogx.dialogs.PopTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class UserSpaceViewModel(
    override val di: DI,
) : ViewModel(), DIAware {

    private val fragment by instance<Fragment>()
    val activity: AppCompatActivity by instance()
    val userStore: UserStore by instance()
    val filterStore: FilterStore by instance()
    private val myPage: MyPage by instance()

    var id: String = ""
        set(value) {
            if (value != field) {
                field = value
                init()
            }
        }

    private val _loading = MutableStateFlow(false);
    val loading: StateFlow<Boolean> get() = _loading

    private val _detailData = MutableStateFlow<SpaceInfo?>(null)
    val detailData: StateFlow<SpaceInfo?> get() = _detailData

    val isSelf get() = userStore.isSelf(id)
    val isFollow get() = detailData.value?.card?.relation?.is_follow == 1

    val tabs = listOf("主页", "动态", "投稿")
    val pagerState = PagerState{ tabs.size }
    val currentPage get() = pagerState.currentPage
    fun init() {
        if (id.isNotBlank()) {
            loadData()
        }
    }

    suspend fun changeTab(index: Int, animate: Boolean = false) {
        if (animate) {
            pagerState.animateScrollToPage(index)
        } else {
            pagerState.scrollToPage(index)
        }
    }

    fun loadData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            _loading.value = true
            val res = UserApi().space(id).awaitCall().gson<ResultInfo<SpaceInfo>>()
            if (res.code == 0) {
                _detailData.value = res.data
            } else {
                PopTip.show(res.message)
            }
        } catch (e: Exception) {
            PopTip.show("网络错误")
            e.printStackTrace()
        } finally {
            _loading.value = false
        }
    }

    fun filterUpperDelete () {
        filterStore.deleteUpper(id.toLong())
    }

    fun filterUpperAdd () {
//        val info = dataInfo
//        if (info == null) {
//            PopTip.show("请等待信息加载完成")
//        } else {
//            filterStore.addUpper(
//                info.card.mid.toLong(),
//                info.card.name,
//            )
//        }
    }

    fun getUserSpaceUrl (): String {
        return "https://space.bilibili.com/${id}"
    }

    fun logout() {
//        MaterialAlertDialogBuilder(activity).apply {
//            setTitle("确定退出登录，喵？")
//            setNegativeButton("确定退出") { dialog, which ->
//                userStore.logout()
//                val nav = (activity as? MainActivity)?.pointerNav?.navController
//                    ?: activity.findNavController(R.id.nav_host_fragment)
//                nav.popBackStack()
//                PopTip.show("已退出登录了喵")
//            }
//            setPositiveButton("取消", null)
//        }.show()
    }

    fun attention() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val data = detailData.value ?: return@launch
            val mode = if (isFollow) { 2 } else { 1 }
            val res = BiliApiService.userRelationApi
                .modify(id, mode)
                .awaitCall().gson<MessageInfo>()
            if (res.code == 0) {
                data.card.relation.is_follow = 2 - mode
                _detailData.value = data
                PopTip.show(if (mode == 1) {
                    "关注成功"
                } else {
                    "已取消关注"
                })
            } else {
                PopTip.show(res.message)
            }
        } catch (e: Exception) {
            PopTip.show("网络错误")
            e.printStackTrace()
        }
    }

    fun toFans() {
        val name = detailData.value?.card?.name ?: return
        fragment.findNavController()
            .navigate(
                Uri.parse("bilimiao://user/follow?id=${id}&type=fans&name=${name}"),
                defaultNavOptions,
            )
    }

    fun toFollow() {
        val nav = fragment.findComposeNavController()
        if (isSelf) {
            nav.navigate(MyFollowPage())
        } else {
            nav.navigate(UserFollowPage()) {
                id set this@UserSpaceViewModel.id
            }
        }
    }

    fun toVideoDetail(item: SpaceInfo.ArchiveItem) {
        fragment.findNavController()
            .navigate(
                Uri.parse("bilimiao://video/" + item.param),
                defaultNavOptions,
            )
    }

    fun toBangumiDetail(item: SpaceInfo.SeasonItem) {
        fragment.findComposeNavController()
            .navigate(BangumiDetailPage()) {
                id set item.param
            }
    }

    fun toFavouriteList() {
        fragment.findComposeNavController()
            .navigate(UserFavouritePage()) {
                id set this@UserSpaceViewModel.id
            }
    }

    fun toFavouriteDetail(item: SpaceInfo.Favourite2Item) {
        fragment.findComposeNavController()
            .navigate(UserFavouriteDetailPage()) {
                id set item.media_id
                title set item.title
            }
    }

}