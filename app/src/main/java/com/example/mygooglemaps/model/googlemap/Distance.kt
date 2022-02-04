package com.example.mygooglemaps.model.googlemap


import com.google.gson.annotations.SerializedName

data class Distance(
    @SerializedName("text")
    val text: String,
    @SerializedName("value")
    val value: Int
)