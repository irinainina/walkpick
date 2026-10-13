package com.walkspot.app.data.local

import androidx.room.AutoMigration
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
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
@TypeConverters(Converters::class)
abstract class WalkPickDatabase : RoomDatabase() {
    abstract fun dailySteps(): DailyStepsDao
    abstract fun walks(): WalkDao
    abstract fun finds(): FindDao
    abstract fun healthEntries(): HealthEntryDao
    abstract fun pendingDeletes(): PendingDeleteDao

    companion object {
        const val NAME = "walkspot.db"
    }
}
