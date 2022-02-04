package com.example.mygooglemaps.di

import com.example.mygooglemaps.network.GoogleMapApiService
import com.example.mygooglemaps.network.TraccarApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val RETROFIT_GOOGLE_MAP = "RetrofitGoogleMap"

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .callTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .baseUrl("http://demo.traccar.org:5055")
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    @Singleton
    @Provides
    @Named(RETROFIT_GOOGLE_MAP)
    fun provideRetrofitGoogleMap(
        okHttpClient: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/maps/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    @Singleton
    @Provides
    fun provideTraccarApiService(retrofit: Retrofit): TraccarApiService =
        retrofit.create(TraccarApiService::class.java)

    @Singleton
    @Provides
    fun provideGoogleMapApiService(@Named(RETROFIT_GOOGLE_MAP) retrofit: Retrofit): GoogleMapApiService =
        retrofit.create(GoogleMapApiService::class.java)
}