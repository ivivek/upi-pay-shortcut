package com.linetra.upishortcut.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * One-time warning that a saved QR can go stale when the shop changes its QR code.
 * Can't be dismissed without ticking the checkbox and tapping Continue.
 */
@Composable
fun RiskDialog(onAccept: () -> Unit) {
    var accepted by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("Before you use saved QR codes") },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Para(
                    rich(
                        "A shortcut pays the UPI ID saved ", "when you scanned",
                        " the shop's QR code. It does not check the QR code the shop is displaying today.",
                    )
                )
                Para(
                    AnnotatedString(
                        "Shops often change their QR code, for example when they switch payment provider " +
                            "or get a new UPI sound box. If you pay using an old saved code:"
                    )
                )
                Bullet(
                    rich(
                        "", "Your money may go to a different account",
                        ", possibly at a different bank, that the shop no longer uses.",
                    )
                )
                Bullet(rich("", "The shop's sound box won't announce your payment", ", so the shop may not know you paid."))
                Para(rich("", "To stay safe:", ""))
                Bullet(
                    rich(
                        "Look at the shop's QR code before paying. If it looks different, ",
                        "delete the saved merchant and scan the new code", ".",
                    )
                )
                Bullet(AnnotatedString("Check the payee name in your UPI app before entering your PIN."))
                Bullet(AnnotatedString("Show the shop your payment success screen."))
                Para(AnnotatedString("UPI payments usually can't be reversed. You use saved QR codes at your own risk."))
                Row(
                    Modifier.fillMaxWidth().clickable { accepted = !accepted },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                    Text(
                        "I understand and accept this risk",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept, enabled = accepted) { Text("Continue") }
        },
    )
}

/** [before] + bold [bold] + [after]. */
private fun rich(before: String, bold: String, after: String): AnnotatedString = buildAnnotatedString {
    append(before)
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
    append(after)
}

@Composable
private fun Para(text: AnnotatedString) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Bullet(text: AnnotatedString) {
    Row {
        Text("•  ", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
