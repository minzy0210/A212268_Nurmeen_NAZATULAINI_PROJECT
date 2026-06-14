package com.example.a212268_nazatulaini_lab1.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 2. DAO — defines all database operations for user-listed items
@Dao
interface UserListedItemDao {

    // Insert a new listing; replaces if same primary key
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: UserListedItemEntity)

    // Observe all listings as a live stream — UI will auto-update
    @Query("SELECT * FROM user_listed_items ORDER BY id DESC")
    fun getAll(): Flow<List<UserListedItemEntity>>

    // Fetch a single item by name (for detail screens)
    @Query("SELECT * FROM user_listed_items WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): UserListedItemEntity?

    // Remove a listing
    @Query("DELETE FROM user_listed_items WHERE name = :name")
    suspend fun deleteByName(name: String)
    // Update an existing listing (e.g. edit stock/condition)
    @Update
    suspend fun update(item: UserListedItemEntity)
}