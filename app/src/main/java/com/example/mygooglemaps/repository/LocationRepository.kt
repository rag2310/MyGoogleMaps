package com.example.mygooglemaps.repository

import android.os.Bundle
import androidx.annotation.WorkerThread
import com.example.mygooglemaps.db.dao.LocationDao
import com.example.mygooglemaps.db.model.LocationEntity
import com.example.mygooglemaps.model.googlemap.DirectionsResponse
import com.example.mygooglemaps.network.GoogleMapApiService
import com.example.mygooglemaps.network.TraccarApiService
import com.example.mygooglemaps.service.locationflow.SharedLocationManager
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Call
import retrofit2.http.Path
import java.util.*
import javax.inject.Inject

class LocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val traccarApiService: TraccarApiService,
    private val googleMapApiService: GoogleMapApiService,
    private val bundle: Bundle,
    private val sharedLocationManager: SharedLocationManager
) {

    @WorkerThread
    fun getLocation() = locationDao.getAll()

    @WorkerThread
    fun getAll() = locationDao.getAllTest()

    @WorkerThread
    suspend fun newLocation(locationEntity: LocationEntity) =
        locationDao.newLocation(locationEntity)

    @WorkerThread
    fun updateLocation(
        idDevices: String,
        locationEntity: LocationEntity,
        batteryLevel: Double
    ) = traccarApiService.postLocation(
        idDevices = idDevices,
        timestamp = locationEntity.timestamp,
        lat = locationEntity.latitude,
        lon = locationEntity.longitude,
        speed = locationEntity.speed,
        bearing = locationEntity.bearing,
        altitude = locationEntity.altitude,
        accuracy = locationEntity.accuracy,
        batt = batteryLevel
    )

    @WorkerThread
    fun getDirections(
        start: LatLng,
        end: LatLng
    ): Call<DirectionsResponse> {
        val startLocation = String.format(Locale.US, "%f,%f", start.latitude, start.longitude)
        val endLocation = String.format(Locale.US, "%f,%f", end.latitude, end.longitude)
        return googleMapApiService.getDirections(
            origin = startLocation,
            destination = endLocation,
            key = bundle.getString("com.google.android.maps.v2.API_KEY").toString()
        )
    }

    val receivingLocationUpdates: StateFlow<Boolean> = sharedLocationManager.receivingLocationUpdate

    fun getLocations() = sharedLocationManager.locationFlow()
}