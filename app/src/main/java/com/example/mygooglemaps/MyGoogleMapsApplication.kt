package com.example.mygooglemaps

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope

@HiltAndroidApp
class MyGoogleMapsApplication : Application() {
    val applicationScope = GlobalScope
}