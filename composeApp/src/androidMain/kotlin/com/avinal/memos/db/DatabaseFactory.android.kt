package com.avinal.memos.db

import android.content.Context
import androidx.room.Room

actual fun createPlatformDatabase(context: Any): MemosDatabase {
    val ctx = context as Context
    return buildDatabase(
        Room.databaseBuilder<MemosDatabase>(
            context = ctx,
            name = ctx.getDatabasePath("memos.db").absolutePath,
        )
    )
}
