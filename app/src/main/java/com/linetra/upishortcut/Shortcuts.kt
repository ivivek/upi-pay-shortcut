package com.linetra.upishortcut

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.linetra.upishortcut.data.Merchant

/** Home-screen shortcuts. Each carries only the merchant id; PayActivity reads the link from the DB. */
object Shortcuts {

    private const val PREFIX = "m-"

    fun id(merchantId: Long) = "$PREFIX$merchantId"

    fun isPinSupported(context: Context) = ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    fun pinnedIds(context: Context): Set<String> =
        ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
            .filter { it.isEnabled }
            .map { it.id }
            .toSet()

    /** Shows the launcher's "Add to home screen?" prompt. Returns false if the launcher refused. */
    fun requestPin(context: Context, merchant: Merchant): Boolean {
        if (!isPinSupported(context)) return false
        val callback = PendingIntent.getBroadcast(
            context,
            merchant.id.toInt(),
            Intent(context, PinnedReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return ShortcutManagerCompat.requestPinShortcut(context, build(context, merchant), callback.intentSender)
    }

    /** Refreshes label/icon of an already pinned or dynamic shortcut. No-op if it was never published. */
    fun update(context: Context, merchant: Merchant) {
        ShortcutManagerCompat.updateShortcuts(context, listOf(build(context, merchant)))
    }

    /** Apps can't remove pinned icons, so disable it: the launcher greys it out and shows [message] on tap. */
    fun onDeleted(context: Context, merchantId: Long) {
        val ids = listOf(id(merchantId))
        ShortcutManagerCompat.removeDynamicShortcuts(context, ids)
        ShortcutManagerCompat.disableShortcuts(context, ids, "Merchant removed from UPI Shortcuts")
    }

    /** Disables shortcuts pinned by the POC build (ids not in the `m-<id>` scheme). */
    fun disableLegacy(context: Context) {
        val legacy = ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
            .filter { it.isEnabled && !it.id.startsWith(PREFIX) }
            .map { it.id }
        if (legacy.isNotEmpty()) {
            ShortcutManagerCompat.disableShortcuts(context, legacy, "Old test shortcut. Remove it and pin again.")
        }
    }

    fun build(context: Context, merchant: Merchant): ShortcutInfoCompat {
        val intent = Intent(context, PayActivity::class.java).apply {
            action = Intent.ACTION_VIEW // shortcut intents must have an action
            putExtra(PayActivity.EXTRA_MERCHANT_ID, merchant.id)
        }
        return ShortcutInfoCompat.Builder(context, id(merchant.id))
            .setShortLabel(merchant.name)
            .setLongLabel("Pay ${merchant.name}")
            .setIcon(IconCompat.createWithAdaptiveBitmap(letterIcon(context, merchant)))
            .setIntent(intent)
            .build()
    }

    /** 108dp adaptive-icon bitmap: merchant's initial on their colour, centred in the safe zone. */
    fun letterIcon(context: Context, merchant: Merchant): Bitmap {
        val size = (108 * context.resources.displayMetrics.density).toInt()
        val bmp = createBitmap(size, size)
        val canvas = Canvas(bmp)
        canvas.drawColor(MerchantColors.argb(merchant.color))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = size * 0.4f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(initial(merchant.name), size / 2f, y, paint)
        return bmp
    }

    fun initial(name: String): String = name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "₹"
}

/** Receives the launcher's confirmation that the user accepted the pin prompt. */
class PinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "Added to home screen", Toast.LENGTH_SHORT).show()
    }
}

object MerchantColors {
    private val palette = longArrayOf(
        0xFF2E7D32, // green
        0xFF1565C0, // blue
        0xFFC62828, // red
        0xFF6A1B9A, // purple
        0xFFEF6C00, // orange
        0xFF00838F, // teal
        0xFF4E342E, // brown
        0xFF37474F, // blue grey
    )

    val count get() = palette.size

    fun argb(index: Int): Int = palette[index.mod(palette.size)].toInt()
}
