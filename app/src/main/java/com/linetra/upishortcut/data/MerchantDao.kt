package com.linetra.upishortcut.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {

    @Query("SELECT * FROM merchants ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Merchant>>

    @Query("SELECT * FROM merchants ORDER BY name COLLATE NOCASE")
    suspend fun all(): List<Merchant>

    @Query("SELECT * FROM merchants WHERE id = :id")
    suspend fun get(id: Long): Merchant?

    /** Widget order: most used first, then alphabetical. */
    @Query("SELECT * FROM merchants ORDER BY useCount DESC, name COLLATE NOCASE")
    suspend fun byUse(): List<Merchant>

    @Query("SELECT * FROM merchants WHERE useCount > 0 ORDER BY useCount DESC, lastUsedAt DESC LIMIT :limit")
    suspend fun mostUsed(limit: Int): List<Merchant>

    @Insert
    suspend fun insert(merchant: Merchant): Long

    @Update
    suspend fun update(merchant: Merchant)

    @Delete
    suspend fun delete(merchant: Merchant)

    @Query("UPDATE merchants SET useCount = useCount + 1, lastUsedAt = :now WHERE id = :id")
    suspend fun recordUse(id: Long, now: Long)
}
