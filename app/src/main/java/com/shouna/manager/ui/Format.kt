package com.shouna.manager.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    fun date(ts: Long?): String =
        if (ts == null) "" else dateFmt.format(Date(ts))

    fun amount(v: Double?): String {
        if (v == null) return ""
        val s = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
        return "¥$s"
    }

    fun expiryHint(expiryDate: Long?, handled: Boolean): String {
        if (handled) return "已处理"
        val e = expiryDate ?: return "无有效期"
        val days = (e - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)
        return when {
            days < 0 -> "已过期 ${-days} 天"
            days == 0L -> "今天到期"
            days <= 7 -> "剩 $days 天"
            else -> "到期 ${date(e)}"
        }
    }
}
