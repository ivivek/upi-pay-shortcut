package com.linetra.upishortcut.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.linetra.upishortcut.Screen
import com.linetra.upishortcut.UpiLink
import com.linetra.upishortcut.data.Merchant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantFormScreen(
    form: Screen.Form,
    others: List<Merchant>,
    pinned: Boolean,
    pinSupported: Boolean,
    newColor: Int,
    onBack: () -> Unit,
    onSave: (Merchant, pin: Boolean) -> Unit,
    onDelete: (Merchant) -> Unit,
) {
    val editing = form.merchant
    var link by rememberSaveable { mutableStateOf(editing?.upiLink ?: form.initialLink) }
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    // Auto-fill the name from the link's payee name until the user types their own.
    var nameTouched by rememberSaveable { mutableStateOf(editing != null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val parsed = remember(link) { UpiLink.parse(link) }
    val valid = parsed as? UpiLink.Valid
    LaunchedEffect(valid?.payeeName, valid?.payee) {
        if (!nameTouched && valid != null) name = valid.payeeName ?: valid.payee
    }
    val duplicate = valid?.let { v ->
        others.firstOrNull { it.id != editing?.id && UpiLink.payeeOf(it.upiLink).equals(v.payee, ignoreCase = true) }
    }

    fun buildMerchant(v: UpiLink.Valid): Merchant {
        val finalName = name.trim().ifEmpty { v.payeeName ?: v.payee }
        return editing?.copy(name = finalName, upiLink = v.link)
            ?: Merchant(name = finalName, upiLink = v.link, color = newColor)
    }

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing == null) "New merchant" else "Edit merchant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (editing != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("UPI link") },
                placeholder = { Text("upi://pay?pa=…") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                isError = link.isNotBlank() && parsed is UpiLink.Invalid,
                supportingText = { if (link.isNotBlank() && parsed is UpiLink.Invalid) Text(parsed.reason) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            if (editing == null) {
                TextButton(onClick = { clipboardText(context)?.let { link = UpiLink.find(it) ?: it } }) {
                    Text("Paste from clipboard")
                }
            }

            if (valid != null) {
                Details(valid)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameTouched = true },
                    label = { Text("Shortcut name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (duplicate != null) {
                    WarningCard("This UPI ID is already saved as \"${duplicate.name}\".")
                }
                for (w in valid.warnings) {
                    when (w) {
                        UpiLink.Warning.AMOUNT_SET -> WarningCard(
                            "This QR has a fixed amount (₹${valid.amount}). It may be for a single bill.",
                            action = "Remove amount" to { link = UpiLink.withoutParam(valid.link, "am") },
                        )
                        UpiLink.Warning.TXN_REF -> WarningCard(
                            "This QR has a transaction reference (tr=${valid.params["tr"]}). " +
                                "One-time QRs may fail when paid again.",
                        )
                        UpiLink.Warning.NO_MERCHANT_CODE -> WarningCard(
                            "No merchant code: this looks like a personal UPI ID. " +
                                "Some UPI apps limit or warn on payments opened from another app.",
                        )
                    }
                }

                val showPin = pinSupported && !pinned
                if (showPin) {
                    Button(onClick = { onSave(buildMerchant(valid), true) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Save & add to home screen")
                    }
                }
                val saveLabel = if (editing != null && pinned) "Save (updates home-screen icon)" else "Save"
                if (showPin) {
                    OutlinedButton(onClick = { onSave(buildMerchant(valid), false) }, modifier = Modifier.fillMaxWidth()) {
                        Text(saveLabel)
                    }
                } else {
                    Button(onClick = { onSave(buildMerchant(valid), false) }, modifier = Modifier.fillMaxWidth()) {
                        Text(saveLabel)
                    }
                }
            }
        }
    }

    if (confirmDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${editing.name}?") },
            text = {
                Text(
                    if (pinned) "Its home-screen icon will stop working (Android doesn't let apps remove it). " +
                        "Long-press the icon to remove it."
                    else "This can't be undone."
                )
            },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(editing) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Details(v: UpiLink.Valid) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DetailRow("Payee", v.payeeName ?: "—")
            DetailRow("UPI ID", v.payee)
            v.merchantCode?.let { DetailRow("Merchant code", it) }
            v.amount?.let { DetailRow("Amount", "₹$it") }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun WarningCard(message: String, action: Pair<String, () -> Unit>? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                if (action != null) {
                    TextButton(onClick = action.second) { Text(action.first) }
                }
            }
        }
    }
}

private fun clipboardText(context: Context): String? {
    val cm = context.getSystemService(ClipboardManager::class.java) ?: return null
    return cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
}
