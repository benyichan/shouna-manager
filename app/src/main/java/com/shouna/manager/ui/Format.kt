package com.shouna.manager.ui

import com.shouna.manager.domain.DAY_MS
import com.shouna.manager.domain.ExpiryCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    fun date(ts: Long?): String =
        if (ts == null) "" else dateFmt.format(Date(ts))

    fun amount(v: Double?): String {
        if (v == null) return ""
        // 先四舍五入到分，避免浮点求和尾巴（如 33.300000000000004）直接透到界面
        val r = Math.round(v * 100.0) / 100.0
        val s = if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
        return "¥$s"
    }

    fun expiryHint(expiryDate: Long?, handled: Boolean): String {
        if (handled) return "已处理"
        val e = expiryDate ?: return "无有效期"
        // 与 ExpiryCalculator.statusOf 同口径按自然日比较，直接用时差整除会在到期日当天出现「今天到期/剩0天」漂移
        val days = (ExpiryCalculator.startOfDay(e) - ExpiryCalculator.startOfDay(System.currentTimeMillis())) / DAY_MS
        return when {
            days < 0 -> "已过期 ${-days} 天"
            days == 0L -> "今天到期"
            days <= 7 -> "剩 $days 天"
            else -> "到期 ${date(e)}"
        }
    }
}
