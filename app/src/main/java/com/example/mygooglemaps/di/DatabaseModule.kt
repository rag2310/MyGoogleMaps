package com.example.mygooglemaps.di

import android.content.Context
import androidx.room.Room
import com.example.mygooglemaps.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context
    ) = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "MY_DATABASE"
    ).fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideLocationDao(appDatabase: AppDatabase) =
        appDatabase.locationDao
}