package com.levana.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalEventDao {
    @Query("SELECT * FROM personal_events ORDER BY hebrewMonth, hebrewDay")
    fun getAll(): Flow<List<PersonalEvent>>

    @Query("SELECT * FROM personal_events WHERE id = :id")
    suspend fun getById(id: Long): PersonalEvent?

    @Query("SELECT * FROM personal_events WHERE hebrewMonth = :month AND hebrewDay = :day")
    suspend fun getByHebrewDate(month: Int, day: Int): List<PersonalEvent>

    @Query("SELECT * FROM personal_events")
    suspend fun getAllOnce(): List<PersonalEvent>

    @Insert
    suspend fun insert(event: PersonalEvent): Long

    @Update
    suspend fun update(event: PersonalEvent)

    @Delete
    suspend fun delete(event: PersonalEvent)
}
