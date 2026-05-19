package com.avinal.memos.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.avinal.memos.db.dao.MemoDao
import com.avinal.memos.db.entity.MemoEntity

@Database(entities = [MemoEntity::class], version = 4)
abstract class MemosDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
}
