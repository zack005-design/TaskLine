package com.example.taskfoundation.data.local.dao

import androidx.room.*
import com.example.taskfoundation.data.local.entity.LibraryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library_items ORDER BY createdAt, id") fun observe(): Flow<List<LibraryItem>>
    @Query("SELECT * FROM library_items ORDER BY createdAt, id") suspend fun all(): List<LibraryItem>
    @Query("SELECT * FROM library_items WHERE id = :id") suspend fun get(id: String): LibraryItem?
    @Upsert suspend fun save(item: LibraryItem)
    @Query("DELETE FROM library_items WHERE id = :id") suspend fun delete(id: String): Int
    @Query("DELETE FROM library_items") suspend fun clear()
    @Insert suspend fun insert(items: List<LibraryItem>)
}
