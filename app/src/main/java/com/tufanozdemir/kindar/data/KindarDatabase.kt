package com.tufanozdemir.kindar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProfileEntity::class, EventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class KindarDatabase : RoomDatabase() {
    abstract fun dao(): KindarDao

    companion object {
        fun create(context: Context): KindarDatabase =
            Room.databaseBuilder(context.applicationContext, KindarDatabase::class.java, "kindar.db")
                .build()
    }
}
