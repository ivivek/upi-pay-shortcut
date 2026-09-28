package com.linetra.upishortcut

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * POC stand-in for the Room database: same SQLite file open + single-row read by id
 * that the real app's shortcut would do, so the launch-time cost is representative.
 */
class MerchantDb(context: Context) : SQLiteOpenHelper(context, "merchants.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE merchants (id INTEGER PRIMARY KEY, name TEXT NOT NULL, upi_link TEXT NOT NULL)")
        db.insert("merchants", null, ContentValues().apply {
            put("id", SEED_ID)
            put("name", MERCHANT_NAME)
            put("upi_link", UPI_LINK)
        })
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun upiLink(id: Long): String? =
        readableDatabase.rawQuery("SELECT upi_link FROM merchants WHERE id = ?", arrayOf(id.toString()))
            .use { c -> if (c.moveToFirst()) c.getString(0) else null }

    companion object {
        const val SEED_ID = 1L
    }
}
