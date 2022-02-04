package com.example.mygooglemaps.model.googlemap


import com.google.gson.annotations.SerializedName

data class Polyline(
    @SerializedName("points")
    val points: String
)