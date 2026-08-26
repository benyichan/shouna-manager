package com.shouna.manager.domain

import java.util.Calendar

/** 历年今日匹配：判断两个时间戳是否同月同日（忽略年份、时区一致用系统默认） */
object SameDate {

    fun isSameMonthDay(a: Long?, b: Long?): Boolean {
        if (a == null || b == null) return false
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH) &&
            ca.get(Calendar.DAY_OF_MONTH) == cb.get(Calendar.DAY_OF_MONTH)
    }

    /** 取某时间戳的年份（用于历年今日展示） */
    fun year(ts: Long): Int = Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.YEAR)
}
