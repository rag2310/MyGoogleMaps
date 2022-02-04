package com.example.mygooglemaps.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import retrofit2.http.Path

@Entity(tableName = "LOCATION", primaryKeys = ["LATITUDE", "LONGITUDE"])
data class LocationEntity(
    @ColumnInfo(name = "TIMESTAMP")
    var timestamp: Long = 0,

    @ColumnInfo(name = "LATITUDE")
    var latitude: Double = 0.0,

    @ColumnInfo(name = "LONGITUDE")
    var longitude: Double = 0.0,

    @ColumnInfo(name = "ALTITUDE")
    var altitude: Double = 0.0,

    @ColumnInfo(name = "IND_SEND")
    var indSend: Int = 0,

    @ColumnInfo(name = "POLYLINE")
    var polyline: String = "",
    var speed: Double = 0.0,
    var bearing: Double = 0.0,
    var accuracy: Double = 0.0,
) {

    override fun toString(): String {
        return "{" +
                "   speed $speed" +
                " bearing $bearing" +
                " accuracy $accuracy" +
                "}"
    }
}
