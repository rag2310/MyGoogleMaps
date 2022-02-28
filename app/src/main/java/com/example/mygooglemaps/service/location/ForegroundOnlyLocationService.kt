package com.example.mygooglemaps.service.location

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.mygooglemaps.db.model.LocationEntity
import com.example.mygooglemaps.model.googlemap.DirectionsResponse
import com.example.mygooglemaps.repository.LocationRepository
import com.example.mygooglemaps.utils.BatteryLevel
import com.example.mygooglemaps.utils.Constants
import com.example.mygooglemaps.utils.toLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundOnlyLocationService : LifecycleService() {

    private var configurationChange = false
    private var serviceRunningInForeground = false
    private val localBinder = LocalBinder()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var locationRequest: LocationRequest

    private lateinit var locationCallback: LocationCallback

    private var currentLocation: Location? = null

    private var compositeDisposable = CompositeDisposable()

    @Inject
    lateinit var locationRepository: LocationRepository

    private var lastLocation: LocationEntity? = null

    override fun onCreate() {
        super.onCreate()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 1 * 1000
            fastestInterval = 1 * 1000
            isWaitForAccurateLocation = true
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                currentLocation = locationResult.lastLocation
                currentLocation.toLocation()?.let { locationModel ->
                    if (lastLocation == null || locationModel.timestamp - lastLocation!!.timestamp >= Constants.INTERVAL) {

                        /*Log.i(
                            TAG,
                            "location new"
                        )*/
                        lastLocation?.let {
                            val diff = locationModel.timestamp - lastLocation!!.timestamp
                            val seg = diff / 1000
                            /*Log.i(
                                TAG,
                                "seg $seg - diff $diff - interval ${Constants.INTERVAL}"
                            )*/
                        }
                        lastLocation = locationModel
//                        sendUpdateLocation(locationModel)
                    } else {
                       /* Log.i(TAG, "location ignored")*/
                        lastLocation?.let {
                            val diff = locationModel.timestamp - lastLocation!!.timestamp
                            val seg = diff / 1000
                            /*Log.i(
                                TAG,
                                "seg $seg - diff $diff - interval ${Constants.INTERVAL}"
                            )*/
                        }
                    }
                    insertLocation(locationModel)
                }
            }
        }
    }

    private fun insertLocation(locationEntity: LocationEntity) {
        /*Log.i(
            TAG,
            "latitude ${locationEntity.latitude}} long ${locationEntity.longitude}, time ${Date()}"
        )*/
        lifecycleScope.launch(Dispatchers.IO) {
            locationRepository.newLocation(locationEntity)
        }
    }

    private fun sendUpdateLocation(locationEntity: LocationEntity) {
        Log.i(TAG, "SEND TRACCAR")
        val idDevices = Constants.ID_DEVICES
        val battery = BatteryLevel.getBatteryLevel(this)
        lifecycleScope.launch(Dispatchers.IO) {
            locationRepository.updateLocation(
                idDevices,
                locationEntity,
                battery
            ).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    Log.i(TAG, response.code().toString())
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.i(TAG, t.message!!)
                }
            })
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent?) {

        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    @SuppressLint("MissingPermission")
    fun subscribeToLocationUpdates() {

        startService(Intent(applicationContext, ForegroundOnlyLocationService::class.java))

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }

    }

    fun unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()")
        try {
            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    stopSelf()
                } else {
                    Log.e(TAG, "Failed to remove Location Callback.")
                }
            }
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    inner class LocalBinder : Binder() {
        internal val service: ForegroundOnlyLocationService
            get() = this@ForegroundOnlyLocationService
    }

    companion object {
        private const val PERIODPERREQUEST = 2L
        private const val TAG = "ForegroundOnlyLocationService"
    }
}