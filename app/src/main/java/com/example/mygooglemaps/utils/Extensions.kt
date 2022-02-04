package com.example.mygooglemaps.utils

import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.room.Ignore
import com.example.mygooglemaps.db.model.LocationEntity
import com.example.mygooglemaps.ui.main.MainActivity
import java.util.*

fun Location?.toLocation(): LocationEntity? {
    return if (this != null) {
        val locationModel = LocationEntity()
        locationModel.altitude = this.altitude
        locationModel.longitude = this.longitude
        locationModel.latitude = this.latitude
        locationModel.timestamp = this.time
        locationModel.speed = this.speed * 1.943844 // speed in knots
        locationModel.bearing = this.bearing.toDouble()
        locationModel.accuracy = this.accuracy.toDouble()
        locationModel
    } else {
        return null
    }
}