package com.linetra.upishortcut

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linetra.upishortcut.ui.AppTheme
import com.linetra.upishortcut.ui.MerchantFormScreen
import com.linetra.upishortcut.ui.MerchantListScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { App(vm) } }
        if (savedInstanceState == null) vm.handleShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.handleShare(intent)
    }

    override fun onResume() {
        super.onResume()
        // The user may have accepted a pin prompt or removed an icon while we were away.
        vm.refresh()
    }
}

@Composable
private fun App(vm: MainViewModel) {
    val merchants by vm.merchants.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::importImage)
    }
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportTo)
    }
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importFrom)
    }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }
    vm.leftoverIcon?.let { name ->
        AlertDialog(
            onDismissRequest = vm::leftoverIconShown,
            title = { Text("Remove the home-screen icon") },
            text = {
                Text(
                    "\"$name\" is deleted, but its icon is still on your home screen. " +
                        "Android doesn't let apps remove icons, so please remove it yourself: " +
                        "long-press the icon and choose Remove."
                )
            },
            confirmButton = { TextButton(onClick = vm::leftoverIconShown) { Text("OK") } },
        )
    }
    when (val screen = vm.screen) {
        Screen.List -> MerchantListScreen(
            merchants = merchants,
            isPinned = vm::isPinned,
            pinSupported = vm.pinSupported,
            snackbar = snackbar,
            onScan = { scope.launch { vm.onScanResult(QrScanner.scanCamera(context)) } },
            onPickImage = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onEnterLink = { vm.openNew() },
            onEdit = vm::openEdit,
            onPin = vm::pin,
            onExport = { exportFile.launch("upi-shortcuts-${LocalDate.now()}.json") },
            onImport = { importFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        )
        is Screen.Form -> MerchantFormScreen(
            form = screen,
            others = merchants,
            pinned = screen.merchant?.let(vm::isPinned) ?: false,
            pinSupported = vm.pinSupported,
            upiApps = vm.upiApps,
            newColor = vm.nextColor(),
            onBack = vm::back,
            onSave = vm::save,
            onDelete = vm::delete,
        )
    }
}
