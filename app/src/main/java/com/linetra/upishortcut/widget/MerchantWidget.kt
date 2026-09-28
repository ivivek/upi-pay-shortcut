package com.linetra.upishortcut.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.linetra.upishortcut.MainActivity
import com.linetra.upishortcut.MerchantColors
import com.linetra.upishortcut.PayActivity
import com.linetra.upishortcut.Shortcuts
import com.linetra.upishortcut.app
import com.linetra.upishortcut.data.Merchant

/** Home-screen widget: a grid of merchants, most used first. Tapping one opens its payment screen. */
class MerchantWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val merchants = context.app.db.merchantDao().byUse()
        provideContent {
            GlanceTheme { Content(context, merchants) }
        }
    }

    @Composable
    private fun Content(context: Context, merchants: List<Merchant>) {
        Box(
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (merchants.isEmpty()) {
                Text(
                    "Add merchants in UPI Shortcuts",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, textAlign = TextAlign.Center),
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
                )
            } else {
                LazyVerticalGrid(gridCells = GridCells.Fixed(4), modifier = GlanceModifier.fillMaxSize()) {
                    items(merchants, itemId = { it.id }) { m -> Cell(context, m) }
                }
            }
        }
    }

    @Composable
    private fun Cell(context: Context, m: Merchant) {
        Column(
            GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable(actionStartActivity(PayActivity.intent(context, m.id))),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Plain coloured circle (rounded on Android 12+) instead of a bitmap, to keep RemoteViews small.
            Box(
                GlanceModifier
                    .size(44.dp)
                    .cornerRadius(22.dp)
                    .background(Color(MerchantColors.argb(m.color))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    Shortcuts.initial(m.name),
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp, fontWeight = FontWeight.Bold),
                )
            }
            Text(
                m.name,
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp, textAlign = TextAlign.Center),
                modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp, start = 2.dp, end = 2.dp),
            )
        }
    }

    companion object {
        /** Re-renders every placed widget; call after merchants change. */
        suspend fun refresh(context: Context) = MerchantWidget().updateAll(context)
    }
}

class MerchantWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MerchantWidget()
}
