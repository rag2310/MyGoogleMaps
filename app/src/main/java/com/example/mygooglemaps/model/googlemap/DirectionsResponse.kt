package com.example.mygooglemaps.model.googlemap


import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    @SerializedName("geocoded_waypoints")
    val geocodedWaypoints: List<GeocodedWaypoint>,
    @SerializedName("routes")
    val routes: List<Route>,
    @SerializedName("status")
    val status: String
)