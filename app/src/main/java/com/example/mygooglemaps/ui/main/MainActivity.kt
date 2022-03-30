package com.example.mygooglemaps.ui.main

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mygooglemaps.R
import com.example.mygooglemaps.model.googlemap.Distance
import com.example.mygooglemaps.model.googlemap.Step
import com.example.mygooglemaps.repository.LocationRepository
import com.example.mygooglemaps.service.location.ForegroundOnlyLocationService
import com.example.mygooglemaps.service.locationflow.TimeRealLocationService
import com.example.mygooglemaps.ui.layers.MyLocationLayer
import com.example.mygooglemaps.utils.*
import com.example.mygooglemaps.utils.SharedPreferenceUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var isFirst: Boolean = true
    private var foregroundOnlyLocationServiceBound = false

    //    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null
    private var timeRealLocationService: TimeRealLocationService? = null
    private lateinit var myLocationLayer: MyLocationLayer
    private var newDistance: Int? = null
    private lateinit var sharedPreferences: SharedPreferences

    private val timeRealLocationServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as TimeRealLocationService.LocalBinder
            timeRealLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
            timeRealLocationService?.subscribeToLocationUpdates()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            timeRealLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    @Inject
    lateinit var repository: LocationRepository

    private var locationFlow: Job? = null

    /*private val foregroundOnlyServiceConnection = object : ServiceConnection {

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
    }*/

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var mMap: GoogleMap
    private var myLocation: LatLng? = null
    private var steps: MutableList<Step> = mutableListOf()
    private var currentStep = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(this.window, false)
        this.window.statusBarColor = Color.TRANSPARENT

        sharedPreferences =
            getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val enabled = sharedPreferences.getBoolean(
            SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false
        )

        if (enabled) {
            unsubscribeToLocationUpdates()
        } else {
            if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                subscribeToLocationUpdates() ?: Log.d(TAG, "Service not Bound")
            } else {
                checkPermission()
            }
        }


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

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        Intent(this, TimeRealLocationService::class.java).also { intent ->
            this.bindService(
                intent,
                timeRealLocationServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            this.unbindService(timeRealLocationServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    private fun createObs() {

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.location.collect {
                        if (it.isNotEmpty()) {
                            val driverLocation = LatLng(it[0].latitude, it[0].longitude)
                            myLocationLayer.marker(locationEntity = it[0])
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    driverLocation,
                                    18.0f
                                )
                            )
                            if (isFirst) {
                                mainViewModel.getDirectionsLocation(locationEntity = it[0])
                                isFirst = false
                            }
                            myLocation = driverLocation
                            getStep()
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
                                            "MyLocationLayer",
                                            "mylocation ${myLocation!!.latitude},${myLocation!!.longitude}"
                                        )
                                        if (it.routes.isNotEmpty()) {
                                            val legs = it.routes[0].legs
                                            val leg = legs[0]
                                            steps.addAll(leg.steps)
                                            myLocationLayer.drawPolyline(leg.steps)
                                            getStep()
                                        }
                                    }
                                }
                                is MainUiState.Error -> {}
                            }
                        }
                }
            }
        }
    }

    private fun getStep() {
        if (steps.isNotEmpty()) {
            if (currentStep <= steps.size) {
                val step = steps[currentStep]
                val distance = FloatArray(1)
                myLocation?.let {
                    myLocationLayer.markerDestiny(
                        LatLng(
                            step.endLocation.lat,
                            step.endLocation.lng
                        )
                    )
                    Location.distanceBetween(
                        it.latitude,
                        it.longitude,
                        step.endLocation.lat,
                        step.endLocation.lng,
                        distance
                    )
                    if (newDistance == null) {
                        newDistance = steps[currentStep].distance.value
                        Log.i(TAG, "newDistance $newDistance")
                    }

                    Log.i(TAG, "distance ${distance[0]}")
                    if (currentStep + 1 <= steps.size) {
                        Log.i(TAG, "maneuver ${steps[currentStep + 1].maneuver}")
                    }

                    if (distance[0] > 0 && distance[0] < 6f) {
                        currentStep += 1
                        newDistance = steps[currentStep + 1].distance.value
                        Log.i(TAG, "newDistance $newDistance")
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
            ActivityCompat.requestPermissions(
                this,
                permissionMissingArray,
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        } else {
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE) {
            for (i in permissions.indices) {
                val permission = permissions[i]
                val grantResult = grantResults[i]
                if (permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        subscribeToLocationUpdates()
                    }
                }
            }
        }
    }

    private fun subscribeToLocationUpdates() {

        locationFlow = repository.getLocations()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                Log.i(TAG, "Foreground location: ${it.toText()}")
                    val driverLocation = LatLng(it.latitude, it.longitude)
                    myLocationLayer.marker(locationEntity = it.toLocation()!!)
                    mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            driverLocation,
                            18.0f
                        )
                    )
                    if (isFirst) {
                        mainViewModel.getDirectionsLocation(locationEntity = it.toLocation()!!)
                        isFirst = false
                    }
                    myLocation = driverLocation
                    getStep()

            }
            .launchIn(lifecycleScope)
    }

    private fun unsubscribeToLocationUpdates() {
        locationFlow?.cancel()
        timeRealLocationService?.unsubscribeToLocationUpdates()
    }

    private companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {

        }
    }
}