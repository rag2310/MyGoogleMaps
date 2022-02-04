package com.example.mygooglemaps.ui.main

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mygooglemaps.R
import com.example.mygooglemaps.service.location.ForegroundOnlyLocationService
import com.example.mygooglemaps.ui.layers.MyLocationLayer
import com.example.mygooglemaps.utils.Converts
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var foregroundOnlyLocationServiceBound = false
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null
    private lateinit var myLocationLayer: MyLocationLayer

    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
            foregroundOnlyLocationService?.subscribeToLocationUpdates()
                ?: Log.d(MainActivity::class.java.simpleName, "Service Not Bound")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var mMap: GoogleMap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(this.window, false)
        this.window.statusBarColor = Color.TRANSPARENT

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        createObs()
        checkPermission()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        myLocationLayer =
            MyLocationLayer(mMap, Converts.bitMapFromVector(this, R.drawable.ic_pin_gps_car))
    }

    override fun onStart() {
        super.onStart()
        Intent(this, ForegroundOnlyLocationService::class.java).also { intent ->
            this.bindService(
                intent,
                foregroundOnlyServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onStop() {
        super.onStop()
        if (foregroundOnlyLocationServiceBound) {
            this.unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
    }

    private fun createObs() {
        /*mainViewModel.location.observe(this) {
            if (it.isNotEmpty()) {
                Log.i(
                    MainActivity::class.java.simpleName,
                    "latitude ${it[0].latitude}} long ${it[0].longitude}, time ${Date()}" +
                            "accuracyCircle ${it[0].accuracy}"
                )

                val driverLocation = LatLng(it[0].latitude, it[0].longitude)
                myLocationLayer.marker(locationEntity = it[0])
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLocation, 18.0f))
            }
        }*/



        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.location.collect {
                        if (it.isNotEmpty()) {
                            Log.i(
                                MainActivity::class.java.simpleName,
                                "latitude ${it[0].latitude}} long ${it[0].longitude}, time ${Date()}"
                            )
                            val driverLocation = LatLng(it[0].latitude, it[0].longitude)
                            myLocationLayer.marker(locationEntity = it[0])
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLocation, 18.0f))
                            //mainViewModel.getDirectionsLocation(locationEntity = it[0])
                        }
                    }
                }

                launch {
                    mainViewModel.directions.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                        .collect { uiState ->
                            when (uiState) {
                                is MainUiState.Success -> {
                                    uiState.directions?.let {
                                        Log.i(
                                            MainActivity::class.java.simpleName,
                                            "polyline ${it.routes[0].overviewPolyline.points},\ntime: ${Date()}"
                                        )
                                    }
                                }
                                is MainUiState.Error -> {}
                            }
                        }
                }
            }
        }
    }


    private fun checkPermission() {
        val permissionMissingList = ArrayList<String>()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionMissingList.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionMissingList.add(Manifest.permission.CAMERA)
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionMissingList.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissionMissingList.size != 0) {
            var permissionMissingArray: Array<String?> = arrayOfNulls(permissionMissingList.size)
            permissionMissingArray = permissionMissingList.toArray(permissionMissingArray)
            ActivityCompat.requestPermissions(this, permissionMissingArray, PERMISSIONS_REQUEST)
        } else {
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST) {
            for (i in permissions.indices) {
                val permission = permissions[i]
                val grantResult = grantResults[i]
                if (permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {

                    }
                }
            }
        }
    }

    private companion object {
        private const val PERMISSIONS_REQUEST = 99
    }
}