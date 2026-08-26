package com.shouna.manager.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shouna.manager.MainActivity
import com.shouna.manager.R

object ReminderNotification {
    private const val CHANNEL_ID = "expiry_reminder"
    private const val NOTIFY_ID = 2001

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "到期提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "物品即将到期/已过期的提醒"
                }
            )
        }
    }

    fun show(context: Context, expired: Int, expiring: Int) {
        ensureChannel(context)
        val text = buildString {
            if (expired > 0) append("$expired 件已过期")
            if (expiring > 0) {
                if (isNotEmpty()) append("，")
                append("$expiring 件即将到期")
            }
            append("，点击查看")
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_EXPIRING, true)
        }
        val pi = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("收纳管家 · 到期提醒")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFY_ID, notification)
        } catch (t: Throwable) {
            android.util.Log.e("Reminder", "notify failed", t)
        }
    }
}
