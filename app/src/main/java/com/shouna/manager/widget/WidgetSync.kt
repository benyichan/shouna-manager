package com.shouna.manager.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 数据变更后刷新桌面小组件。
 * 小组件自身只在系统轮询（小时级）或被添加时才重算，增删改物品后必须显式触发，
 * 否则临期/补货数字会长期停留在旧值。fire-and-forget，未添加小组件时 updateAll 为 no-op。
 */
object WidgetSync {
    fun refresh(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { StockWidget().updateAll(context.applicationContext) }
        }
    }
}
