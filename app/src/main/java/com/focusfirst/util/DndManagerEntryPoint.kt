package com.focusfirst.util

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DndManagerEntryPoint {
    fun dndManager(): DndManager
}
