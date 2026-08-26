package com.shouna.manager.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shouna.manager.data.AppGraph
import com.shouna.manager.domain.DAY_MS
import com.shouna.manager.domain.ExpiryCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_DAILY) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = ExpiryCalculator.startOfDay(System.currentTimeMillis())
                // 每件物品用自己的提前量窗口；取全局最大窗口(30天)粗筛，再逐件精判
                val coarseTo = today + MAX_LEAD_DAYS * DAY_MS
                val candidates = AppGraph.itemRepository.getNeedRemind(coarseTo)
                val toRemind = candidates.filter { item ->
                    if (item.expiryDate == null) return@filter false
                    val lead = item.remindDaysBefore?.toLong() ?: DEFAULT_LEAD_DAYS
                    val expiryDay = ExpiryCalculator.startOfDay(item.expiryDate)
                    expiryDay <= today + lead * DAY_MS
                }
                if (toRemind.isNotEmpty()) {
                    val expired = toRemind.count { it.expiryDate!! < today }
                    val expiring = toRemind.count { it.expiryDate!! >= today }
                    ReminderNotification.show(context, expired, expiring)
                }
            } catch (t: Throwable) {
                android.util.Log.e("Reminder", "daily check failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val DEFAULT_LEAD_DAYS: Long = 7
        const val MAX_LEAD_DAYS: Long = 30
    }
}
