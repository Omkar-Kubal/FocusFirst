package com.focusfirst.di

import android.content.Context
import androidx.room.Room
import com.focusfirst.data.db.FocusDatabase
import com.focusfirst.data.db.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that wires the Room database and its DAOs into the DI graph.
 *
 * Both bindings are @Singleton:
 *   - [FocusDatabase] — one Room instance per process lifetime.
 *   - [SessionDao]    — stateless DAO backed by the singleton database.
 *
 * Usage in a ViewModel or Service:
 *   @Inject lateinit var sessionDao: SessionDao
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFocusDatabase(
        @ApplicationContext context: Context,
    ): FocusDatabase = Room.databaseBuilder(
        context,
        FocusDatabase::class.java,
        "focus_db",
    ).build()

    @Provides
    @Singleton
    fun provideSessionDao(database: FocusDatabase): SessionDao =
        database.sessionDao()
}
