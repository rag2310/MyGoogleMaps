package com.example.mygooglemaps.ui.layers

import android.graphics.Color
import com.example.mygooglemaps.db.model.LocationEntity
import com.example.mygooglemaps.utils.Converts
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*

class MyLocationLayer(
    private val map: GoogleMap,
    private val markerResource: BitmapDescriptor
) {

    /*private var markerDescriptor: BitmapDescriptor =
        BitmapDescriptorFactory.fromResource(markerResource)*/
    private var drawAccuracy = true
    private val accuracyStrokeColor: Int = Color.argb(255, 130, 182, 228)
    private val accuracyFillColor: Int = Color.argb(100, 130, 182, 228)


    private var positionMarker: Marker? = null
    private var accuracyCircle: Circle? = null

    fun marker(locationEntity: LocationEntity) {

        positionMarker?.remove()

        val positionMarkerOptions = MarkerOptions()
            .position(LatLng(locationEntity.latitude, locationEntity.longitude))
            .icon(markerResource)
            .anchor(0.5f, 0.5f)
        positionMarker = map.addMarker(positionMarkerOptions)

        accuracyCircle?.remove()

        if (drawAccuracy) {
            val accuracyCircleOptions = CircleOptions()
                .center(LatLng(locationEntity.latitude, locationEntity.longitude))
                .radius(locationEntity.accuracy)
                .fillColor(accuracyFillColor)
                .strokeColor(accuracyStrokeColor)
                .strokeWidth(2.0f)
            accuracyCircle = map.addCircle(accuracyCircleOptions)
        }
    }
}