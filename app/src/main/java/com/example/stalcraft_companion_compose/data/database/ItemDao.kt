package com.example.stalcraft_companion_compose.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.example.stalcraft_companion_compose.data.models.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
  @Insert(onConflict = REPLACE)
  fun insertAll(items: List<Item>)

  @Insert(onConflict = REPLACE)
  fun insert(item: Item)

  @Query("SELECT * FROM items")
  fun getAllItems(): Flow<List<Item>>

  @Query("SELECT * FROM items WHERE id = :itemId")
  fun getItemById(itemId: String): Item?

  @Query("DELETE FROM items")
  fun clearAll()
}