package com.example.mygooglemaps.service.locationflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.mygooglemaps.R
import com.example.mygooglemaps.repository.LocationRepository
import com.example.mygooglemaps.ui.main.MainActivity
import com.example.mygooglemaps.utils.SharedPreferenceUtil
import com.example.mygooglemaps.utils.toText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class TimeRealLocationService : LifecycleService() {

    private var configurationChange = false
    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager

    private var currentLocation: Location? = null

    @Inject
    lateinit var repository: LocationRepository

    private var locationFlow: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate()")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand()")

        val cancelLocationTrackingFromNotification =
            intent?.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)

        if (cancelLocationTrackingFromNotification == true) {
            unsubscribeToLocationUpdates()
            stopSelf()
        }
        return super.onStartCommand(intent, flags, START_NOT_STICKY)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.i(TAG, "onBind()")
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent?) {
        Log.i(TAG, "onRebind()")
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind()")
        if (!configurationChange && SharedPreferenceUtil.getLocationTrackingPref(this)) {
            Log.i(TAG, "Start foreground service")
            val notification = generateNotification(currentLocation)
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }

        return true
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    fun subscribeToLocationUpdates() {
        Log.i(TAG, "subscribeToLocationUpdates()")
        SharedPreferenceUtil.saveLocationTrackingPref(this, true)
        startService(Intent(applicationContext, TimeRealLocationService::class.java))

        locationFlow = repository.getLocations()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                Log.i(TAG, "Service location: ${it.toText()}")
                currentLocation = it

                if (serviceRunningInForeground) {
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        generateNotification(currentLocation)
                    )
                }
            }
            .launchIn(lifecycleScope)
    }

    fun unsubscribeToLocationUpdates() {
        Log.i(TAG, "unsubscribeToLocationUpdates()")
        locationFlow?.cancel()
        SharedPreferenceUtil.saveLocationTrackingPref(this, false)
    }

    private fun generateNotification(location: Location?): Notification {

        Log.i(TAG, "generateNotification()")

        val mainNotificationText = location?.toText() ?: getString(R.string.no_location_text)
        val titleText = getText(R.string.app_name)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationManager.createNotificationChannel(notificationChannel)
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)

        val launchActivityIntent = Intent(this, MainActivity::class.java)

        val cancelIntent = Intent(this, TimeRealLocationService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, 0
        )

        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder.setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_pin_gps_car, "init",
                activityPendingIntent
            )
            .addAction(
                R.drawable.ic_pin_gps_car,
                "cancelar", servicePendingIntent
            )
            .build()
    }

    inner class LocalBinder : Binder() {
        internal val service: TimeRealLocationService
            get() = this@TimeRealLocationService
    }

    companion object {
        private const val TAG = "TimeRealLocationService"

        private const val PACKAGE_NAME = "com.example.android.whileinuselocation"

        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"
    }
}