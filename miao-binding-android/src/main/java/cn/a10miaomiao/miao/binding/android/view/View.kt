package cn.a10miaomiao.miao.binding.android.view

import android.view.View
import androidx.annotation.Px
import cn.a10miaomiao.miao.binding.exception.BindingOnlySetException
import cn.a10miaomiao.miao.binding.miaoEffect


inline var View._visibility: Int
    @Deprecated(NO_GETTER, level = DeprecationLevel.HIDDEN) get() = noGetter
    set(value) = miaoEffect(value) {
        visibility = it
    }

inline var View._show: Boolean
    @Deprecated(NO_GETTER, level = DeprecationLevel.HIDDEN) get() = noGetter
    set(value) = miaoEffect(value) {
        visibility = if (it) View.VISIBLE else View.GONE
    }

inline var View._isEnabled: Boolean
    @Deprecated(NO_GETTER, level = DeprecationLevel.HIDDEN) get() = noGetter
    set(value) = miaoEffect(value) {
        isEnabled = it
    }

