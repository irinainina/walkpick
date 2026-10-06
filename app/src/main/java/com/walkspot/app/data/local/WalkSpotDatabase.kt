package com.walkspot.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DailyStepsEntity::class,
        WalkEntity::class,
        FindEntity::class,
        HealthEntryEntity::class,
        PendingDeleteEntity::class,
    ],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class WalkSpotDatabase : RoomDatabase() {
    abstract fun dailySteps(): DailyStepsDao
    abstract fun walks(): WalkDao
    abstract fun finds(): FindDao
    abstract fun healthEntries(): HealthEntryDao
    abstract fun pendingDeletes(): PendingDeleteDao

    companion object {
        const val NAME = "walkspot.db"
    }
}
