package com.example.mygooglemaps.db.dao

import androidx.room.*
import com.example.mygooglemaps.db.model.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM LOCATION")
    fun getAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM LOCATION")
    fun getAllTest(): Flow<List<LocationEntity>>

    @Query("DELETE FROM LOCATION")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(locationEntity: LocationEntity)

    @Transaction
    suspend fun newLocation(locationEntity: LocationEntity){
        deleteAll()
        insert(locationEntity)
    }
}