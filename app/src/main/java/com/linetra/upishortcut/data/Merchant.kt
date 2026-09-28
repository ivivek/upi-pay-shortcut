package com.linetra.upishortcut.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchants")
data class Merchant(
    // Room uses AUTOINCREMENT, so ids (and the shortcut ids built from them) are never reused.
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Full `upi://pay?...` link exactly as scanned or entered. */
    val upiLink: String,
    /** Package of the UPI app to open, or null for the system default / chooser. */
    val upiPackage: String? = null,
    /** Index into [com.linetra.upishortcut.MerchantColors]. */
    val color: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
    val useCount: Int = 0,
)
