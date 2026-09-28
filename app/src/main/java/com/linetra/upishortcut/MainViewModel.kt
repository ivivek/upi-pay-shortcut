package com.linetra.upishortcut

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linetra.upishortcut.data.Merchant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Screen {
    data object List : Screen

    /** Add (merchant == null) or edit form; [initialLink] pre-fills a new merchant. */
    data class Form(val merchant: Merchant?, val initialLink: String = "") : Screen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
    private val dao = context.app.db.merchantDao()

    val merchants: StateFlow<List<Merchant>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var screen by mutableStateOf<Screen>(Screen.List)
        private set

    var upiApps by mutableStateOf(emptyList<UpiAppInfo>())
        private set

    /** Shortcut ids currently pinned; read from the launcher, not stored, so removals show up. */
    var pinnedIds by mutableStateOf(emptySet<String>())
        private set

    val pinSupported = Shortcuts.isPinSupported(context)

    /** One-off message for a snackbar (scan failures etc.). */
    var message by mutableStateOf<String?>(null)
        private set

    fun messageShown() {
        message = null
    }

    init {
        Shortcuts.disableLegacy(context)
        context.deleteDatabase("merchants.db") // POC's database
    }

    /** Called on resume: pins, installed UPI apps and usage may all have changed while we were away. */
    fun refresh() {
        refreshPinned()
        upiApps = UpiApps.installed(context)
        viewModelScope.launch { publishDynamic() }
    }

    private suspend fun publishDynamic() {
        Shortcuts.publishDynamic(context, dao.mostUsed(4), dao.all())
    }

    private fun refreshPinned() {
        pinnedIds = Shortcuts.pinnedIds(context)
    }

    fun isPinned(merchant: Merchant) = Shortcuts.id(merchant.id) in pinnedIds

    fun openNew(link: String = "") {
        screen = Screen.Form(null, link)
    }

    fun openEdit(merchant: Merchant) {
        screen = Screen.Form(merchant)
    }

    fun onScanResult(result: ScanResult) {
        when (result) {
            // Open the form even for non-UPI text so the user sees what was scanned and why it's rejected.
            is ScanResult.Found -> openNew(UpiLink.find(result.text) ?: result.text)
            is ScanResult.Failed -> message = result.message
            ScanResult.Cancelled -> Unit
        }
    }

    fun importImage(uri: Uri) {
        viewModelScope.launch { onScanResult(QrScanner.decodeImage(context, uri)) }
    }

    /** Handles "Share to UPI Shortcuts" with either text containing a link or a QR image. */
    fun handleShare(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND) return
        val link = intent.getStringExtra(Intent.EXTRA_TEXT)?.let(UpiLink::find)
        val image = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        when {
            link != null -> openNew(link)
            image != null -> importImage(image)
            else -> message = "No UPI link found in what was shared"
        }
    }

    fun back() {
        screen = Screen.List
    }

    /** New merchants get the next colour in the palette so neighbouring icons differ. */
    fun nextColor(): Int = merchants.value.size % MerchantColors.count

    fun save(merchant: Merchant, pin: Boolean) {
        viewModelScope.launch {
            val saved = if (merchant.id == 0L) merchant.copy(id = dao.insert(merchant)) else merchant.also { dao.update(it) }
            Shortcuts.update(context, saved)
            if (pin) Shortcuts.requestPin(context, saved)
            publishDynamic()
            screen = Screen.List
        }
    }

    fun pin(merchant: Merchant) {
        Shortcuts.requestPin(context, merchant)
    }

    fun delete(merchant: Merchant) {
        viewModelScope.launch {
            dao.delete(merchant)
            Shortcuts.onDeleted(context, merchant.id)
            refreshPinned()
            publishDynamic()
            screen = Screen.List
        }
    }
}
