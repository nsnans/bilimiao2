package com.a10miaomiao.bilimiao.page.video.comment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import bilibili.main.community.reply.v1.ReplyOuterClass
import cn.a10miaomiao.miao.binding.android.view._bottomPadding
import cn.a10miaomiao.miao.binding.android.view._leftPadding
import cn.a10miaomiao.miao.binding.android.view._rightPadding
import cn.a10miaomiao.miao.binding.android.view._topPadding
import com.a10miaomiao.bilimiao.MainNavGraph
import com.a10miaomiao.bilimiao.comm.*
import com.a10miaomiao.bilimiao.comm.entity.video.VideoCommentReplyInfo
import com.a10miaomiao.bilimiao.comm.mypage.MyPage
import com.a10miaomiao.bilimiao.comm.mypage.myPageConfig
import com.a10miaomiao.bilimiao.comm.recycler.*
import com.a10miaomiao.bilimiao.comm.utils.*
import com.a10miaomiao.bilimiao.commponents.comment.VideoCommentViewContent
import com.a10miaomiao.bilimiao.commponents.comment.videoCommentView
import com.a10miaomiao.bilimiao.commponents.loading.ListState
import com.a10miaomiao.bilimiao.commponents.loading.listStateView
import com.a10miaomiao.bilimiao.config.config
import com.a10miaomiao.bilimiao.store.WindowStore
import com.a10miaomiao.bilimiao.widget.expandabletext.ExpandableTextView
import com.a10miaomiao.bilimiao.widget.expandabletext.app.LinkType
import com.a10miaomiao.bilimiao.widget.gridimage.NineGridImageView
import com.a10miaomiao.bilimiao.widget.gridimage.OnImageItemClickListener
import com.chad.library.adapter.base.listener.OnItemClickListener
import kotlinx.coroutines.launch
import net.mikaelzero.mojito.Mojito
import net.mikaelzero.mojito.impl.DefaultPercentProgress
import net.mikaelzero.mojito.impl.NumIndicator
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import splitties.toast.toast
import splitties.views.backgroundColor
import splitties.views.dsl.core.*
import splitties.views.dsl.recyclerview.recyclerView

class VideoCommentDetailFragment : Fragment(), DIAware, MyPage {

    override val pageConfig = myPageConfig {
        title = "评论详情"
    }

    override val di: DI by lazyUiDi(ui = { ui })

    private val viewModel by diViewModel<VideoCommentDetailViewModel>(di)

    private val windowStore by instance<WindowStore>()

    private var mAdapter: MiaoBindingAdapter<ReplyOuterClass.ReplyInfo>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.coroutineScope.launch {
            windowStore.connectUi(ui)
        }
        // 页面返回回调
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val nav = findNavController()
                    nav.previousBackStackEntry?.let {
                        it.savedStateHandle[MainNavGraph.args.reply] = viewModel.reply
                    }
                    nav.popBackStack()
                }
            })
    }

    private fun toSelfLink (view: View, url: String) {
        val urlInfo = BiliUrlMatcher.findIDByUrl(url)
        val urlType = urlInfo[0]
        var urlId = urlInfo[1]
        if (urlType == "BV") {
            urlId = "BV$urlId"
        }
        val args = bundleOf(
            MainNavGraph.args.id to urlId
        )
        when(urlType){
            "AV", "BV" -> {
                args.putString(MainNavGraph.args.type, urlType)
                Navigation.findNavController(view)
                    .navigate(MainNavGraph.action.videoCommentDetail_to_videoInfo, args)
            }
        }
    }

    private val handleUserClick = View.OnClickListener {
        val id = it.tag
        if (id != null && id is String) {
            val args = bundleOf(
                MainNavGraph.args.id to id
            )
            Navigation.findNavController(it)
                .navigate(MainNavGraph.action.videoCommentDetail_to_user, args)
        }
    }

    private val handleRefresh = SwipeRefreshLayout.OnRefreshListener {
        viewModel.refreshList()
    }

    private val handleItemClick = OnItemClickListener { adapter, view, position ->
    }

    private val handleRootLikeClick = View.OnClickListener {
        viewModel.setRootLike()
    }

    private val handleLikeClick = View.OnClickListener {
        val index = it.tag
        if (index is Int && index >= 0) {
            viewModel.setLike(index) { item ->
                viewModel.list.data[index] = item
                mAdapter?.setData(index, item)
            }
        }
    }

    private val handleLinkClickListener = ExpandableTextView.OnLinkClickListener { view, linkType, content, selfContent -> //根据类型去判断
        when (linkType) {
            LinkType.LINK_TYPE -> {
                val url = content
                val re = BiliNavigation.navigationTo(view, url)
                if (!re) {
                    if (url.indexOf("bilibili://") == 0) {
                        toast("不支持打开的链接：$url")
                    } else {
                        BiliUrlMatcher.toUrlLink(view, url)
                    }
                }
            }
            LinkType.MENTION_TYPE -> {
//                toast("你点击了@用户 内容是：$content")
            }
            LinkType.SELF -> {
                toSelfLink(view, selfContent)
            }
        }
    }

    private val handleImageItemClick = object : OnImageItemClickListener {
        override fun onClick(
            nineGridView: NineGridImageView,
            imageView: ImageView,
            url: String,
            urlList: List<String>,
            externalPosition: Int,
            position: Int
        ) {
            Mojito.start(imageView.context) {
                urls(urlList)
                position(position)
                progressLoader {
                    DefaultPercentProgress()
                }
                setIndicator(NumIndicator())
                views(nineGridView.getImageViews().toTypedArray())
                mojitoListener(
                    onLongClick = { activity, _, _, _, i ->
                        val imageUrl = urlList[i]
                        ImageSaveUtil(activity!!, imageUrl).showMemu()
                    }
                )
            }
        }
    }

    val itemUi = miaoBindingItemUi<ReplyOuterClass.ReplyInfo> { item, index ->
        videoCommentView(
            index = index,
            mid = item.mid,
            uname = item.member.name,
            avatar = item.member.face,
            time = NumberUtil.converCTime(item.ctime),
            location = item.replyControl.location,
            floor = 0,
            content = VideoCommentViewContent(
                message = item.content.message,
                emote = item.content.emoteMap.values.map {
                    VideoCommentViewContent.Emote(
                        it.id, it.text, it.url
                    )
                },
                picturesList = item.content.picturesList.map { UrlUtil.autoHttps(it.imgSrc) },
            ),
            like = item.like,
            count = item.count,
            isLike = item.replyControl.action == 1L,
            textIsSelectable = true,
            onUpperClick = handleUserClick,
            onLinkClick = handleLinkClickListener,
            onLikeClick = handleLikeClick,
            onImageItemClick = handleImageItemClick,
        ).apply {
            layoutParams = ViewGroup.LayoutParams(matchParent, wrapContent)
        }
    }

    val ui = miaoBindingUi {
        val contentInsets = windowStore.getContentInsets(parentView)

        recyclerView {
            _leftPadding = contentInsets.left
            _rightPadding = contentInsets.right

            backgroundColor = config.windowBackgroundColor
            _miaoLayoutManage(
                LinearLayoutManager(requireContext())
            )

            mAdapter = _miaoAdapter(
                items = viewModel.list.data,
                itemUi = itemUi,
            ) {
                stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                setOnItemClickListener(handleItemClick)
                loadMoreModule.setOnLoadMoreListener {
                    viewModel.loadMode()
                }
            }

            headerViews(mAdapter!!) {
                val reply = viewModel.reply
                +videoCommentView(
                    mid = reply.mid,
                    uname = reply.uname,
                    avatar = reply.avatar,
                    time = NumberUtil.converCTime(reply.ctime),
                    location = reply.location,
                    floor = reply.floor,
                    content = reply.content,
                    like = reply.like,
                    count = reply.count,
                    isLike = reply.action == 1L,
                    textIsSelectable = true,
                    onUpperClick = handleUserClick,
                    onLinkClick = handleLinkClickListener,
                    onLikeClick = handleRootLikeClick,
                    onImageItemClick = handleImageItemClick,
                ).apply {
                    _topPadding = contentInsets.top + config.dividerSize
                    backgroundColor = config.blockBackgroundColor
                }..lParams(matchParent, matchParent)
                +textView {
                    text = "全部回复"
                }..lParams {
                    margin = config.dividerSize
                }
            }

            footerViews(mAdapter!!) {
                +listStateView(
                    when {
                        viewModel.triggered -> ListState.NORMAL
                        viewModel.list.loading -> ListState.LOADING
                        viewModel.list.fail -> ListState.FAIL
                        viewModel.list.finished -> ListState.NOMORE
                        else -> ListState.NORMAL
                    }
                ) {
                    _bottomPadding = contentInsets.bottom
                }..lParams(matchParent, wrapContent)
            }
        }.wrapInSwipeRefreshLayout {
            setColorSchemeResources(config.themeColorResource)
            setOnRefreshListener(handleRefresh)
            _isRefreshing = viewModel.triggered
        }
    }

}