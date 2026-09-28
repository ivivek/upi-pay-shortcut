package com.linetra.upishortcut.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.linetra.upishortcut.UpiLink
import com.linetra.upishortcut.data.Merchant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantListScreen(
    merchants: List<Merchant>,
    isPinned: (Merchant) -> Boolean,
    pinSupported: Boolean,
    onEnterLink: () -> Unit,
    onEdit: (Merchant) -> Unit,
    onPin: (Merchant) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("UPI Shortcuts") }) },
        floatingActionButton = {
            if (merchants.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onEnterLink,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Enter link") },
                )
            }
        },
    ) { padding ->
        if (merchants.isEmpty()) {
            EmptyState(Modifier.padding(padding), onEnterLink)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding),
            ) {
                if (!pinSupported) {
                    item {
                        Text(
                            "Your launcher doesn't support adding shortcuts to the home screen.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                items(merchants, key = { it.id }) { m ->
                    MerchantRow(m, isPinned(m), pinSupported, onEdit = { onEdit(m) }, onPin = { onPin(m) })
                }
            }
        }
    }
}

@Composable
private fun MerchantRow(
    merchant: Merchant,
    pinned: Boolean,
    pinSupported: Boolean,
    onEdit: () -> Unit,
    onPin: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MerchantAvatar(merchant.name, merchant.color)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        merchant.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        UpiLink.payeeOf(merchant.upiLink) ?: "Invalid link",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (pinned) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "On home screen",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    FilledTonalButton(onClick = onPin, enabled = pinSupported, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add to home screen")
                    }
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onEnterLink: () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No merchants yet", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.padding(top = 8.dp))
        Text(
            "Save a shop's UPI QR once and pay it with one tap from your home screen.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 24.dp))
        Button(onClick = onEnterLink, modifier = Modifier.fillMaxWidth()) { Text("Enter UPI link") }
    }
}
