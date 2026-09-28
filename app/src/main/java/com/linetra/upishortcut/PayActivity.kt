package com.linetra.upishortcut

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Invisible trampoline launched by home-screen shortcuts: looks up the merchant by id,
 * fires its upi:// link at the chosen UPI app (or the system default) and finishes.
 */
class PayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val t0 = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)

        val merchantId = intent.getLongExtra(EXTRA_MERCHANT_ID, -1)
        lifecycleScope.launch {
            val merchant = app.db.merchantDao().get(merchantId)
            val tLookup = SystemClock.elapsedRealtime()
            if (merchant == null) {
                Toast.makeText(this@PayActivity, "This merchant was deleted", Toast.LENGTH_LONG).show()
            } else {
                launchUpi(merchant.upiLink, merchant.upiPackage)
                ShortcutManagerCompat.reportShortcutUsed(this@PayActivity, Shortcuts.id(merchant.id))
                app.scope.launch { app.db.merchantDao().recordUse(merchant.id, System.currentTimeMillis()) }
            }
            Log.i(
                TAG,
                "processStart→onCreate=${t0 - Process.getStartElapsedRealtime()}ms " +
                    "lookup=${tLookup - t0}ms total=${SystemClock.elapsedRealtime() - t0}ms"
            )
            finish()
        }
    }

    private fun launchUpi(link: String, pkg: String?) {
        val pay = Intent(Intent.ACTION_VIEW, link.toUri())
        if (pkg != null) {
            try {
                startActivity(Intent(pay).setPackage(pkg))
                return
            } catch (e: ActivityNotFoundException) {
                // Chosen app was uninstalled: fall back to the system default below.
            }
        }
        try {
            startActivity(pay)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No UPI app installed", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "UpiShortcut"
        const val EXTRA_MERCHANT_ID = "merchant_id"
    }
}
