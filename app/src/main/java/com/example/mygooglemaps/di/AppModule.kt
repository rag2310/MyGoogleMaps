package com.example.mygooglemaps.di

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import com.example.mygooglemaps.MyGoogleMapsApplication
import com.example.mygooglemaps.service.locationflow.SharedLocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideMetaData(@ApplicationContext context: Context): Bundle =
        context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        ).metaData

    @Provides
    @Singleton
    fun provideSharedLocationManager(@ApplicationContext context: Context): SharedLocationManager =
        SharedLocationManager(context,
            (context.applicationContext as MyGoogleMapsApplication).applicationScope)
}