package com.example.mygooglemaps.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mygooglemaps.db.dao.LocationDao
import com.example.mygooglemaps.db.model.LocationEntity

@Database(entities = [LocationEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract val locationDao: LocationDao
}