package com.walkpick.app.data

import com.walkpick.app.data.health.healthEntry
import com.walkpick.app.data.local.HealthKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class HealthTest {

    private val now = Instant.parse("2026-10-06T18:00:00Z")
    private val morning = Instant.parse("2026-10-06T08:10:00Z")

    private fun entry(kind: HealthKind, value: Double, diastolic: Int? = null, at: Instant = morning) =
        healthEntry(kind, value, diastolic, at, now)

    @Test
    fun anEntryKeepsWhatWasMeasuredAndIsNotSyncedYet() {
        val weight = entry(HealthKind.WEIGHT, 64.2)!!
        assertEquals(HealthKind.WEIGHT, weight.kind)
        assertEquals(64.2, weight.value)
        assertEquals(morning, weight.measuredAt)
        assertEquals(false, weight.synced)
        assertNotEquals(weight.id, entry(HealthKind.WEIGHT, 64.2)!!.id)
    }

    @Test
    fun valuesNobodyCanHaveAreRefused() {
        assertNull(entry(HealthKind.WEIGHT, 6.4))
        assertNull(entry(HealthKind.GLUCOSE, 55.0))
        assertNull(entry(HealthKind.MOOD, 0.0))
        assertNull(entry(HealthKind.MOOD, 3.5))
        assertEquals(5.0, entry(HealthKind.MOOD, 5.0)!!.value)
        assertEquals(5.4, entry(HealthKind.GLUCOSE, 5.4)!!.value)
    }

    @Test
    fun pressureNeedsBothNumbersAndTheLowerOneBelowTheUpper() {
        assertEquals(78, entry(HealthKind.PRESSURE, 122.0, 78)!!.diastolic)
        assertNull(entry(HealthKind.PRESSURE, 122.0))
        assertNull(entry(HealthKind.PRESSURE, 122.0, 130))
        assertNull(entry(HealthKind.PRESSURE, 122.0, 20))
        assertNull(entry(HealthKind.PRESSURE, 122.5, 78))
        assertNull(entry(HealthKind.WEIGHT, 64.2, 78))
    }

    @Test
    fun aMomentThatHasNotComeYetIsRefused() {
        assertNull(entry(HealthKind.WEIGHT, 64.2, at = now.plusSeconds(60)))
        assertEquals(now, entry(HealthKind.WEIGHT, 64.2, at = now)!!.measuredAt)
    }
}
