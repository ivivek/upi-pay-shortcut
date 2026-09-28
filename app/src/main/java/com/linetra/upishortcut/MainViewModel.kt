package com.linetra.upishortcut

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linetra.upishortcut.data.Merchant
import com.linetra.upishortcut.widget.MerchantWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException

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

    private val prefs = context.getSharedPreferences("settings", Application.MODE_PRIVATE)

    /** Whether the user accepted the one-time "saved QR codes can go stale" warning. */
    var riskAccepted by mutableStateOf(prefs.getBoolean(KEY_RISK_ACCEPTED, false))
        private set

    fun acceptRisk() {
        prefs.edit { putBoolean(KEY_RISK_ACCEPTED, true) }
        riskAccepted = true
    }

    /** Name of a just-deleted merchant whose home-screen icon the user must remove by hand. */
    var leftoverIcon by mutableStateOf<String?>(null)
        private set

    fun leftoverIconShown() {
        leftoverIcon = null
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

    /** Everything outside the app that mirrors the merchant list. */
    private suspend fun merchantsChanged() {
        publishDynamic()
        MerchantWidget.refresh(context)
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
            merchantsChanged()
            screen = Screen.List
        }
    }

    fun pin(merchant: Merchant) {
        Shortcuts.requestPin(context, merchant)
    }

    fun delete(merchant: Merchant) {
        val wasPinned = isPinned(merchant)
        viewModelScope.launch {
            dao.delete(merchant)
            Shortcuts.onDeleted(context, merchant.id)
            // Android doesn't let apps remove pinned icons, so tell the user to do it.
            if (wasPinned) leftoverIcon = merchant.name
            refreshPinned()
            merchantsChanged()
            screen = Screen.List
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val merchants = dao.all()
            message = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(Backup.toJson(merchants)) }
                }
                "Exported ${merchants.size} merchants"
            } catch (e: IOException) {
                "Export failed: ${e.message}"
            }
        }
    }

    /** Adds merchants from an export, skipping links that are already saved. Doesn't pin anything. */
    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            message = try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                }
                val incoming = Backup.fromJson(text)
                val existing = dao.all().map { it.upiLink }.toMutableSet()
                var added = 0
                for (m in incoming) {
                    if (existing.add(m.upiLink)) {
                        dao.insert(m)
                        added++
                    }
                }
                merchantsChanged()
                val skipped = incoming.size - added
                "Imported $added merchants" + if (skipped > 0) " ($skipped already saved)" else ""
            } catch (e: IOException) {
                "Import failed: ${e.message}"
            } catch (e: JSONException) {
                "That file isn't a UPI Shortcuts export"
            } catch (e: IllegalArgumentException) {
                e.message
            }
        }
    }

    private companion object {
        const val KEY_RISK_ACCEPTED = "risk_accepted"
    }
}
