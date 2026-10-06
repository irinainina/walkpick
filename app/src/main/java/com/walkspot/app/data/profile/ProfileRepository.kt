package com.walkspot.app.data.profile

import com.walkspot.app.data.local.DailyStepsDao
import com.walkspot.app.data.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val store: ProfileStore,
    private val days: DailyStepsDao,
    private val sync: SyncScheduler,
) {
    val profile: Flow<Profile> = store.value

    suspend fun change(transform: (Profile) -> Profile) {
        var goal: Int? = null
        store.change { old -> transform(old).also { if (it.dailyGoal != old.dailyGoal) goal = it.dailyGoal } }
        // Today takes the new goal at once; the days already over keep the goal they had.
        goal?.let { days.setGoal(LocalDate.now(), it, Instant.now()) }
        sync.pushSoon()
    }
}
