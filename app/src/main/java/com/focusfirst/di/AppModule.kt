package com.focusfirst.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.focusfirst.data.SettingsRepository
import com.focusfirst.data.focusFirstSettingsDataStore
import com.focusfirst.data.db.FocusDatabase
import com.focusfirst.data.db.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that wires the Room database, its DAOs, and the DataStore
 * settings repository into the DI graph.
 *
 * All bindings are [@Singleton]: one instance per process lifetime.
 *
 * Usage in a ViewModel or Service:
 *   @Inject lateinit var sessionDao: SessionDao
 *   @Inject lateinit var settingsRepository: SettingsRepository
 *
 * Note: [com.focusfirst.billing.BillingManager] is provided automatically by
 * Hilt via its @Inject constructor — no explicit @Provides entry is required here.
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

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.focusFirstSettingsDataStore

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context,
        dataStore: DataStore<Preferences>,
    ): SettingsRepository = SettingsRepository(
        appContext = context.applicationContext,
        dataStore  = dataStore,
    )
}
