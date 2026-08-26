package com.shouna.manager.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import com.shouna.manager.MainActivity
import com.shouna.manager.data.AppGraph
import com.shouna.manager.domain.DAY_MS
import com.shouna.manager.domain.ExpiryCalculator
import kotlinx.coroutines.runBlocking

/** 桌面小组件：临期 / 需补货数量，点击进入临期页 */
class StockWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = ExpiryCalculator.startOfDay(System.currentTimeMillis())
        val to = today + 7 * DAY_MS
        val expired = runBlocking { AppGraph.itemRepository.getExpired(today).size }
        val expiring = runBlocking { AppGraph.itemRepository.getExpiring(today, to).size }
        val restock = runBlocking { AppGraph.itemRepository.getNeedRestock().size }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF2E9E76)))
                        .padding(12.dp)
                        .clickable(
                            actionStartActivity(
                                Intent(context, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .putExtra(MainActivity.EXTRA_OPEN_EXPIRING, true)
                            )
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        androidx.glance.text.Text(
                            text = "收纳管家",
                            style = androidx.glance.text.TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp)
                        )
                        androidx.glance.text.Text(
                            text = if (expired + expiring > 0) "临期 $expiring · 过期 $expired" else "暂无临期物品",
                            style = androidx.glance.text.TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 16.sp
                            )
                        )
                        androidx.glance.text.Text(
                            text = if (restock > 0) "需补货 $restock 件" else "补货清单已齐",
                            style = androidx.glance.text.TextStyle(
                                color = ColorProvider(Color(0xFFDFF5EA)),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class StockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StockWidget()
}
