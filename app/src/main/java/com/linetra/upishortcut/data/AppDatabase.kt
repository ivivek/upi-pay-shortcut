package com.linetra.upishortcut.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Merchant::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun merchantDao(): MerchantDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "upi_shortcut.db").build()
    }
}
