package com.linetra.upishortcut

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    /** Shortcut ids currently pinned; read from the launcher, not stored, so removals show up. */
    var pinnedIds by mutableStateOf(emptySet<String>())
        private set

    val pinSupported = Shortcuts.isPinSupported(context)

    init {
        Shortcuts.disableLegacy(context)
        context.deleteDatabase("merchants.db") // POC's database
    }

    fun refreshPinned() {
        pinnedIds = Shortcuts.pinnedIds(context)
    }

    fun isPinned(merchant: Merchant) = Shortcuts.id(merchant.id) in pinnedIds

    fun openNew(link: String = "") {
        screen = Screen.Form(null, link)
    }

    fun openEdit(merchant: Merchant) {
        screen = Screen.Form(merchant)
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
            screen = Screen.List
        }
    }
}
