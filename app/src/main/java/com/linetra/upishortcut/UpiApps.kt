package com.linetra.upishortcut

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri

data class UpiAppInfo(val packageName: String, val label: String)

object UpiApps {

    /** Installed apps that handle upi://pay (needs the <queries> entry in the manifest on Android 11+). */
    fun installed(context: Context): List<UpiAppInfo> {
        val pm = context.packageManager
        val probe = Intent(Intent.ACTION_VIEW, "upi://pay".toUri())
        return pm.queryIntentActivities(probe, PackageManager.MATCH_ALL)
            .map { UpiAppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
