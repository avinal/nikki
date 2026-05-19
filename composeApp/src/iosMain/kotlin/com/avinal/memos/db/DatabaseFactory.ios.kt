package com.avinal.memos.db

import androidx.room.Room

actual fun createPlatformDatabase(context: Any): MemosDatabase {
    val dbPath = NSHomeDirectory() + "/Documents/memos.db"
    return buildDatabase(
        Room.databaseBuilder<MemosDatabase>(name = dbPath)
    )
}

private fun NSHomeDirectory(): String =
    platform.Foundation.NSHomeDirectory()
