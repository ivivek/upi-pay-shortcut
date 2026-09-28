package com.linetra.upishortcut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linetra.upishortcut.ui.AppTheme
import com.linetra.upishortcut.ui.MerchantFormScreen
import com.linetra.upishortcut.ui.MerchantListScreen

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { App(vm) } }
    }

    override fun onResume() {
        super.onResume()
        // The user may have accepted a pin prompt or removed an icon while we were away.
        vm.refreshPinned()
    }
}

@Composable
private fun App(vm: MainViewModel) {
    val merchants by vm.merchants.collectAsStateWithLifecycle()
    when (val screen = vm.screen) {
        Screen.List -> MerchantListScreen(
            merchants = merchants,
            isPinned = vm::isPinned,
            pinSupported = vm.pinSupported,
            onEnterLink = { vm.openNew() },
            onEdit = vm::openEdit,
            onPin = vm::pin,
        )
        is Screen.Form -> MerchantFormScreen(
            form = screen,
            others = merchants,
            pinned = screen.merchant?.let(vm::isPinned) ?: false,
            pinSupported = vm.pinSupported,
            newColor = vm.nextColor(),
            onBack = vm::back,
            onSave = vm::save,
            onDelete = vm::delete,
        )
    }
}
