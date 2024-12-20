package com.a10miaomiao.bilimiao.page

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorDestinationBuilder
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import cn.a10miaomiao.bilimiao.compose.pages.download.DownloadListPage
import cn.a10miaomiao.bilimiao.compose.pages.dynamic.DynamicPage
import cn.a10miaomiao.miao.binding.android.view.*
import cn.a10miaomiao.miao.binding.miaoEffect
import com.a10miaomiao.bilimiao.MainActivity
import com.a10miaomiao.bilimiao.R
import com.a10miaomiao.bilimiao.comm.*
import com.a10miaomiao.bilimiao.comm.delegate.player.BasePlayerDelegate
import com.a10miaomiao.bilimiao.comm.dsl.addOnDoubleClickTabListener
import com.a10miaomiao.bilimiao.comm.mypage.*
import com.a10miaomiao.bilimiao.comm.navigation.FragmentNavigatorBuilder
import com.a10miaomiao.bilimiao.comm.navigation.navigateToCompose
import com.a10miaomiao.bilimiao.comm.navigation.openSearch
import com.a10miaomiao.bilimiao.comm.recycler.RecyclerViewFragment
import com.a10miaomiao.bilimiao.comm.store.UserStore
import com.a10miaomiao.bilimiao.comm.utils.miaoLogger
import com.a10miaomiao.bilimiao.page.user.HistoryFragment
import com.a10miaomiao.bilimiao.store.WindowStore
import com.a10miaomiao.bilimiao.widget.scaffold.getScaffoldView
import com.a10miaomiao.bilimiao.widget.wrapInViewPager2Container
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kongzue.dialogx.dialogs.PopTip
import kotlinx.coroutines.launch
import org.kodein.di.*
import splitties.experimental.InternalSplittiesApi
import splitties.views.dsl.core.*


class MainFragment : Fragment(), DIAware, MyPage {

    companion object : FragmentNavigatorBuilder() {
        override val name = "main"

        override fun FragmentNavigatorDestinationBuilder.init() {
            deepLink("bilimiao://home")
        }

        private val ID_viewPager = View.generateViewId()
        private val ID_tabLayout = View.generateViewId()
        private val ID_space = View.generateViewId()
    }

    private var pageTitle = "bilimiao\n-\n首页"

    override val pageConfig = myPageConfig {
        title = pageTitle
        menu = myMenu {
            checkable = true
            checkedKey = MenuKeys.home
            myItem {
                key = MenuKeys.dynamic
                title = "动态"
                iconResource = R.drawable.ic_baseline_icecream_24
            }
            myItem {
                key = MenuKeys.home
                title = "首页"
                iconResource = R.drawable.ic_baseline_home_24
            }
            myItem {
                key = MenuKeys.menu
                title = "菜单"
                iconResource = R.drawable.ic_baseline_menu_24
            }
        }
    }

    override fun onMenuItemClick(view: View, menuItem: MenuItemPropInfo) {
        super.onMenuItemClick(view, menuItem)
        val nav = (activity as? MainActivity)?.pointerNav?.navController
            ?: requireActivity().findNavController(R.id.nav_host_fragment)
        when (menuItem.key) {
            MenuKeys.dynamic -> {
//                val navOptions = NavOptions.Builder()
//                    .setLaunchSingleTop(true) // 设置singleTop属性
//                    .setPopUpTo(MainFragment.id, false, true) // 三个参数分别为popUpToId, popUpToInclusive, popUpToSaveState
//                    .setRestoreState(true) // 设置restoreState属性
//                    .build()
//                nav.navigateToCompose(DynamicPage(), navOptions)
            }
        }
    }
    override val di: DI by lazyUiDi(ui = { ui })

    private val viewModel by diViewModel<MainViewModel>(di)
    private val windowStore by instance<WindowStore>()
    private val playerDelegate by instance<BasePlayerDelegate>()
    private val scaffoldApp by lazy { requireActivity().getScaffoldView() }


    private val userStore by instance<UserStore>()

    private var backKeyPressedTimes = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui.parentView = container
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
//        showPrivacyDialog()
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val now = System.currentTimeMillis()
                    if (scaffoldApp.showPlayer) {
                        if (now - backKeyPressedTimes > 2000) {
                            PopTip.show("再按一次退出播放")
                            backKeyPressedTimes = now
                        } else {
                            playerDelegate.closePlayer()
                            backKeyPressedTimes = 0
                        }
                    } else {
                        if (now - backKeyPressedTimes > 2000) {
                            PopTip.show("再按一次退出bilimiao")
                            backKeyPressedTimes = now
                        } else {
                            requireActivity().finish()
                        }
                    }
                }
            })
    }

    private fun showPrivacyDialog() {
        val appInfo = requireActivity().packageManager.getApplicationInfo(
            requireActivity().application.packageName,
            PackageManager.GET_META_DATA
        )
        if (appInfo.metaData.getString("BaiduMobAd_CHANNEL") != "Coolapk") {
            return
        }
        val sp = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        if (sp.getBoolean("is_approve_privacy", false)) {
            return
        }
        val dialog = MaterialAlertDialogBuilder(requireActivity()).apply {
            setTitle("温馨提示")
            setMessage("根据相关政策法规，你需要先阅读并同意《隐私协议》才能使用本软件")
            setCancelable(false)
            setNeutralButton("阅读《隐私协议》", null)
            setPositiveButton("同意"){ dialog, _ ->
                sp.edit()
                    .putBoolean("is_approve_privacy", true)
                    .apply()
                dialog.dismiss()
            }
            setNegativeButton("拒绝") { _, _ ->
                requireActivity().finish()
            }
        }.show()
        // 手动设置按钮点击事件，可阻止对话框自动关闭
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://10miaomiao.cn/bilimiao/privacy.html")
            requireActivity().startActivity(intent)
        }
    }

    private fun initView(view: View) {
        val tabLayout = view.findViewById<TabLayout>(ID_tabLayout)
        val viewPager = view.findViewById<ViewPager2>(ID_viewPager)
        val space = view.findViewById<Space>(ID_space)
        if (viewPager.adapter == null) {
            val mAdapter = object : FragmentStateAdapter(childFragmentManager, lifecycle) {
                override fun getItemCount() = viewModel.navList.size

                override fun getItemId(position: Int): Long {
                    return viewModel.navList[position].id
                }

                override fun containsItem(itemId: Long): Boolean {
                    return viewModel.navList.indexOfFirst { it.id == itemId } != -1
                }

                override fun createFragment(position: Int): Fragment {
                    return viewModel.navList[position].createFragment()
                }
            }
            viewPager.adapter = mAdapter
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = viewModel.navList[position].title
            }.attach()
            tabLayout.addOnDoubleClickTabListener {
                val itemId = mAdapter.getItemId(it.position)
                childFragmentManager.findFragmentByTag("f$itemId")?.let { currentFragment ->
                    if (currentFragment is RecyclerViewFragment) {
                        currentFragment.toListTop()
                    }
                }
            }
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val title = viewModel.navList[position].title
                    pageTitle = "bilimiao\n-\n$title"
                    pageConfig.notifyConfigChanged()
                }
            })
        }
        lifecycleScope.launch {
            viewModel.navListFlow.collect {
                if (it.size > 1) {
                    space.visibility = View.GONE
                    tabLayout.visibility = View.VISIBLE
                } else {
                    space.visibility = View.VISIBLE
                    tabLayout.visibility = View.GONE
                }
                viewPager.adapter?.notifyDataSetChanged()
            }
        }
    }

    @OptIn(InternalSplittiesApi::class)
    val ui = miaoBindingUi {
        connectStore(viewLifecycleOwner, windowStore)
        connectStore(viewLifecycleOwner, userStore)
        val contentInsets = windowStore.getContentInsets(parentView)
        miaoEffect(userStore.isLogin()) {
            pageConfig.notifyConfigChanged()
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                    parentView?.let(::initView)
                }
            }
        }
        verticalLayout {
            views {
                +space(ID_space) {
                    visibility = View.GONE
                }..lParams {
                    _height = contentInsets.top
                }
                +tabLayout(ID_tabLayout) {
                    _topPadding = contentInsets.top
                    _leftPadding = contentInsets.left
                    _rightPadding = contentInsets.right
                    tabMode = TabLayout.MODE_SCROLLABLE
                    tabGravity = TabLayout.GRAVITY_CENTER
                }..lParams(matchParent, wrapContent)
                +view<ViewPager2>(ID_viewPager) {
                    _leftPadding = contentInsets.left
                    _rightPadding = contentInsets.right
                    offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
                    isSaveEnabled = false
                }.wrapInViewPager2Container {
                }..lParams(matchParent, matchParent) {
                    weight = 1f
                }
            }
        }
    }


}