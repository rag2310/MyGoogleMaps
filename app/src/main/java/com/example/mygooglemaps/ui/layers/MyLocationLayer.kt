package com.example.mygooglemaps.ui.layers

import android.graphics.Color
import android.location.Location
import android.util.Log
import com.example.mygooglemaps.db.model.LocationEntity
import com.example.mygooglemaps.model.googlemap.Step
import com.example.mygooglemaps.utils.Converts
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import android.location.Location.distanceBetween

class MyLocationLayer(
    private val map: GoogleMap,
    private val markerResource: BitmapDescriptor
) {
    private var drawAccuracy = true
    private val accuracyStrokeColor: Int = Color.argb(255, 130, 182, 228)
    private val accuracyFillColor: Int = Color.argb(100, 130, 182, 228)


    private var positionMarker: Marker? = null
    private var accuracyCircle: Circle? = null
    private var destinyMarker: Marker? = null
    private var drawPolyLines: MutableList<Polyline> = mutableListOf()
    private var drawPolyline: Polyline? = null

    private var waiting = false
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

    fun drawPolyline(steps: List<Step>) {
        if (!waiting) {
            waiting = true
            drawPolyline?.remove()
            drawPolyLines.forEach {
                it.remove()
            }
            drawPolyLines.clear()

            steps.forEach { step ->
                step.polyline.let { polyline ->
                    val points = PolyUtil.decode(polyline.points)
                    val polylineOptions = PolylineOptions()
                    polylineOptions.addAll(points)
                    polylineOptions.color(Color.RED)
                    drawPolyline = map.addPolyline(polylineOptions)
                    drawPolyline?.let {
                        drawPolyLines.add(it)
                        it.width = 20f
                    }
                }
            }
            waiting = false
        }
    }

    fun markerDestiny(destiny: LatLng) {
        destinyMarker?.remove()

        val positionMarkerOptions = MarkerOptions()
            .position(destiny)
            .title("Destino")
        destinyMarker = map.addMarker(positionMarkerOptions)
    }


    fun removeAllMarkers() {
        drawPolyline?.remove()
        drawPolyLines.forEach {
            it.remove()
        }
        drawPolyLines.clear()
        destinyMarker?.remove()
        accuracyCircle?.remove()
        positionMarker?.remove()
    }

    companion object {
        private val TAG = MyLocationLayer::class.java.simpleName
    }
}