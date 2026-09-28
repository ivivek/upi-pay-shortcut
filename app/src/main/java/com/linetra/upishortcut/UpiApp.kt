package com.linetra.upishortcut

import android.app.Application
import android.content.Context
import com.linetra.upishortcut.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class UpiApp : Application() {

    val db: AppDatabase by lazy { AppDatabase.create(this) }

    /** For work that must outlive a screen, e.g. recording a shortcut tap after PayActivity finishes. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

val Context.app: UpiApp get() = applicationContext as UpiApp
