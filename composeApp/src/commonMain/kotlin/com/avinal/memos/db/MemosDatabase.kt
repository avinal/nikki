package com.avinal.memos.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.avinal.memos.db.dao.MemoDao
import com.avinal.memos.db.dao.PendingSyncDao
import com.avinal.memos.db.entity.MemoEntity
import com.avinal.memos.db.entity.PendingSyncEntity

@Database(entities = [MemoEntity::class, PendingSyncEntity::class], version = 5)
abstract class MemosDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun pendingSyncDao(): PendingSyncDao
}
