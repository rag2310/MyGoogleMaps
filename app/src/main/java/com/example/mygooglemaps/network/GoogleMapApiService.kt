package com.example.mygooglemaps.network

import com.example.mygooglemaps.model.googlemap.DirectionsResponse
import com.google.android.gms.maps.model.LatLng
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleMapApiService {

    @GET("directions/json")
    fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("sensor") sensor: Boolean = true,
        @Query("key") key: String
    ): Call<DirectionsResponse>
}