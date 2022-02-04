package com.example.mygooglemaps.model.googlemap


import com.google.gson.annotations.SerializedName

data class Step(
    @SerializedName("distance")
    val distance: Distance,
    @SerializedName("duration")
    val duration: Duration,
    @SerializedName("end_location")
    val endLocation: EndLocation,
    @SerializedName("html_instructions")
    val htmlInstructions: String,
    @SerializedName("maneuver")
    val maneuver: String,
    @SerializedName("polyline")
    val polyline: Polyline,
    @SerializedName("start_location")
    val startLocation: StartLocation,
    @SerializedName("travel_mode")
    val travelMode: String
)