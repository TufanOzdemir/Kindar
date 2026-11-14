package com.tufanozdemir.kindar.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KindarDao {

    @Transaction
    @Query("SELECT * FROM profiles ORDER BY displayName COLLATE NOCASE")
    fun observeProfilesWithEvents(): Flow<List<ProfileWithEvents>>

    @Transaction
    @Query("SELECT * FROM profiles WHERE id = :id")
    fun observeProfileWithEvents(id: Long): Flow<ProfileWithEvents?>

    @Query("SELECT * FROM profiles")
    suspend fun allProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM events")
    suspend fun allEvents(): List<EventEntity>

    @Query("SELECT * FROM profiles WHERE contactLookupKey IS NOT NULL")
    suspend fun contactProfiles(): List<ProfileEntity>

    @Insert
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Insert
    suspend fun insertProfiles(profiles: List<ProfileEntity>)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfiles(profiles: List<ProfileEntity>)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Insert
    suspend fun insertEvent(event: EventEntity): Long

    @Insert
    suspend fun insertEvents(events: List<EventEntity>)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events")
    suspend fun clearEvents()

    @Query("DELETE FROM profiles")
    suspend fun clearProfiles()
}
