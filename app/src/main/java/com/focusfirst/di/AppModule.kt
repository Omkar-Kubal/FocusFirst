package com.focusfirst.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.focusfirst.data.SettingsRepository
import com.focusfirst.data.focusFirstSettingsDataStore
import com.focusfirst.data.db.FocusDatabase
import com.focusfirst.data.db.MIGRATION_1_2
import com.focusfirst.data.db.SessionDao
import com.focusfirst.data.db.TaskDao
import com.focusfirst.service.SoundManager
import com.focusfirst.util.DndManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that wires the Room database, its DAOs, the DataStore
 * settings repository, SoundManager, and DndManager into the DI graph.
 *
 * All bindings are [@Singleton]: one instance per process lifetime.
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
    ).addMigrations(MIGRATION_1_2)
     .build()

    @Provides
    @Singleton
    fun provideSessionDao(database: FocusDatabase): SessionDao =
        database.sessionDao()

    @Provides
    @Singleton
    fun provideTaskDao(database: FocusDatabase): TaskDao =
        database.taskDao()

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

    @Provides
    @Singleton
    fun provideSoundManager(
        @ApplicationContext context: Context,
    ): SoundManager = SoundManager(context.applicationContext)

    @Provides
    @Singleton
    fun provideDndManager(
        @ApplicationContext context: Context,
    ): DndManager = DndManager(context.applicationContext)
}
