package com.example.mygooglemaps.network

import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Query

interface TraccarApiService {

    @POST("/")
    fun postLocation(
        @Query("id") idDevices: String,
        @Query("timestamp") timestamp: Long,
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("speed") speed: Double,
        @Query("bearing") bearing: Double,
        @Query("altitude") altitude: Double,
        @Query("accuracy") accuracy: Double,
        @Query("batt") batt: Double
    ): Call<Void>
}