package com.linetra.upishortcut

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri

/**
 * Invisible trampoline launched by home-screen shortcuts.
 * Fires the upi:// link at the chosen UPI app (or the system default) and finishes.
 * The link comes either embedded in the shortcut or looked up in the DB by merchant id.
 */
class PayActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val t0 = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)

        val merchantId = intent.getLongExtra(EXTRA_MERCHANT_ID, -1)
        val mode: String
        val upiUri: String? = if (merchantId >= 0) {
            mode = "db"
            MerchantDb(this).use { it.upiLink(merchantId) }
        } else {
            mode = "embedded"
            intent.getStringExtra(EXTRA_UPI_URI)
        }
        val tLookup = SystemClock.elapsedRealtime()

        if (upiUri != null) {
            launchUpi(this, upiUri, intent.getStringExtra(EXTRA_PACKAGE))
        } else {
            Toast.makeText(this, "Merchant not found", Toast.LENGTH_LONG).show()
        }
        val tDone = SystemClock.elapsedRealtime()

        // Process start → onCreate shows cold-start cost; lookup is the DB (or extra) read.
        Log.i(
            TAG,
            "mode=$mode processStart→onCreate=${t0 - Process.getStartElapsedRealtime()}ms " +
                "lookup=${tLookup - t0}ms startActivity=${tDone - tLookup}ms total=${tDone - t0}ms"
        )
        finish()
    }

    companion object {
        private const val TAG = "UpiShortcut"
        const val EXTRA_UPI_URI = "upi_uri"
        const val EXTRA_MERCHANT_ID = "merchant_id"
        const val EXTRA_PACKAGE = "upi_package"

        fun launchUpi(activity: Activity, upiUri: String, pkg: String?) {
            val pay = Intent(Intent.ACTION_VIEW, upiUri.toUri())
            if (pkg != null) pay.setPackage(pkg)
            try {
                activity.startActivity(pay)
            } catch (e: ActivityNotFoundException) {
                val msg = if (pkg != null) "$pkg is not installed" else "No UPI app installed"
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
