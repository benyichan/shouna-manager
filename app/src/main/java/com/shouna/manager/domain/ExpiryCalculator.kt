package com.shouna.manager.domain

import com.shouna.manager.data.db.entity.ItemEntity
import java.util.Calendar

const val DAY_MS: Long = 24 * 60 * 60 * 1000L
const val EXPIRING_LEAD_DAYS: Long = 7

object ExpiryCalculator {

    /**
     * 计算物品有效日期。
     * 直填有效日期时用直填值；否则用「生产日期 + 保质期天数」。
     */
    fun computeExpiry(
        productionDate: Long?,
        shelfLifeDays: Long?,
        directExpiry: Long?
    ): Long? {
        if (directExpiry != null) return directExpiry
        if (productionDate == null || shelfLifeDays == null) return null
        return productionDate + shelfLifeDays * DAY_MS
    }

    /**
     * 到期状态：已处理 > 已过期 > 临期(≤leadDays天) > 正常。无有效日期视为正常。
     */
    fun statusOf(
        expiryDate: Long?,
        handled: Boolean,
        leadDays: Long = EXPIRING_LEAD_DAYS,
        now: Long = System.currentTimeMillis()
    ): ExpiryStatus {
        if (handled) return ExpiryStatus.HANDLED
        val e = expiryDate ?: return ExpiryStatus.NORMAL
        val days = (startOfDay(e) - startOfDay(now)) / DAY_MS
        return when {
            days < 0 -> ExpiryStatus.EXPIRED
            days <= leadDays -> ExpiryStatus.EXPIRING
            else -> ExpiryStatus.NORMAL
        }
    }

    /** 按物品计算状态：自动应用每件自定义提前量（remindDaysBefore，默认 7 天） */
    fun statusOf(item: ItemEntity, now: Long = System.currentTimeMillis()): ExpiryStatus =
        statusOf(item.expiryDate, item.handled, item.remindDaysBefore?.toLong() ?: EXPIRING_LEAD_DAYS, now)

    fun startOfDay(ts: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
