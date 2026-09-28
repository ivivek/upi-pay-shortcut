package com.linetra.upishortcut

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri

// POC: hard-coded merchant QR link (spaces encoded so the URI parses cleanly).
const val MERCHANT_NAME = "Example Services"
const val UPI_LINK =
    "upi://pay?ver=01&pa=examplestore@okbank&pn=Example%20Services&tn=%20&am=&mode=00" +
        "&purpose=00&orgid=000000&sign=&mc=5411"

class MainActivity : Activity() {

    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        setContentView(ScrollView(this).apply { addView(content) })
        // Create/seed the DB now so the DB shortcuts never pay the one-time schema creation cost.
        MerchantDb(this).use { it.readableDatabase }
        render()
    }

    private fun render() {
        content.addView(text("Merchant: $MERCHANT_NAME", 18f, bold = true))
        content.addView(text(UPI_LINK, 12f).apply { setTextIsSelectable(true) })

        val pinSupported = ShortcutManagerCompat.isRequestPinShortcutSupported(this)
        content.addView(text("Launcher supports pinned shortcuts: $pinSupported", 14f))

        section("System default UPI app (chooser if none set)")
        content.addView(button("Pay now") { PayActivity.launchUpi(this, UPI_LINK, null) })
        content.addView(button("Pin shortcut: default app (embedded link)") {
            pinShortcut("default", MERCHANT_NAME, null, useDb = false)
        })
        content.addView(button("Pin shortcut: default app (DB lookup)") {
            pinShortcut("default-db", "$MERCHANT_NAME DB", null, useDb = true)
        })

        val apps = installedUpiApps()
        section("Installed UPI apps (${apps.size})")
        if (apps.isEmpty()) content.addView(text("None found", 14f))
        for ((pkg, label) in apps) {
            content.addView(text("$label\n$pkg", 14f, bold = true))
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(button("Pay now") { PayActivity.launchUpi(this, UPI_LINK, pkg) }, weight())
            row.addView(button("Pin") { pinShortcut(pkg, "$MERCHANT_NAME · $label", pkg, useDb = false) }, weight())
            row.addView(button("Pin (DB)") {
                pinShortcut("$pkg-db", "$MERCHANT_NAME DB · $label", pkg, useDb = true)
            }, weight())
            content.addView(row)
        }
    }

    private fun installedUpiApps(): List<Pair<String, String>> {
        val probe = Intent(Intent.ACTION_VIEW, "upi://pay".toUri())
        return packageManager.queryIntentActivities(probe, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
    }

    private fun pinShortcut(idSuffix: String, label: String, pkg: String?, useDb: Boolean) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            toast("This launcher doesn't support pinned shortcuts")
            return
        }
        val intent = Intent(this, PayActivity::class.java).apply {
            action = Intent.ACTION_VIEW // shortcut intents must have an action
            if (useDb) {
                putExtra(PayActivity.EXTRA_MERCHANT_ID, MerchantDb.SEED_ID)
            } else {
                putExtra(PayActivity.EXTRA_UPI_URI, UPI_LINK)
            }
            pkg?.let { putExtra(PayActivity.EXTRA_PACKAGE, it) }
        }
        val info = ShortcutInfoCompat.Builder(this, "example-$idSuffix")
            .setShortLabel(label.take(25))
            .setLongLabel("Pay $label")
            .setIcon(IconCompat.createWithAdaptiveBitmap(letterIcon(MERCHANT_NAME, useDb)))
            .setIntent(intent)
            .build()
        val requested = ShortcutManagerCompat.requestPinShortcut(this, info, null)
        if (!requested) toast("Pin request failed")
    }

    /** 108dp adaptive-icon bitmap with the merchant's initial centred in the safe zone. */
    private fun letterIcon(name: String, useDb: Boolean): Bitmap {
        val size = dp(108)
        val bmp = createBitmap(size, size)
        val canvas = Canvas(bmp)
        // Green = embedded link, blue = DB lookup, so the two variants are easy to tell apart.
        canvas.drawColor(if (useDb) Color.rgb(0x15, 0x65, 0xC0) else Color.rgb(0x2E, 0x7D, 0x32))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.4f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(name.first().uppercase(), size / 2f, y, paint)
        return bmp
    }

    private fun section(title: String) {
        content.addView(View(this), LinearLayout.LayoutParams(1, dp(16)))
        content.addView(text(title, 16f, bold = true))
    }

    private fun text(s: String, sizeSp: Float, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = sizeSp
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun weight() = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
