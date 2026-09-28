package com.linetra.upishortcut.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.linetra.upishortcut.MerchantColors
import com.linetra.upishortcut.Shortcuts

/** Same initial-on-colour look as the home-screen icon. */
@Composable
fun MerchantAvatar(name: String, color: Int, size: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(MerchantColors.argb(color)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            Shortcuts.initial(name),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
