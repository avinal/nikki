package com.avinal.memos.db

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

fun buildDatabase(builder: RoomDatabase.Builder<MemosDatabase>): MemosDatabase =
    builder
        .fallbackToDestructiveMigration(true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

expect fun createPlatformDatabase(context: Any): MemosDatabase
