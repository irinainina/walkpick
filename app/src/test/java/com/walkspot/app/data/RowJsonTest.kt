package com.walkspot.app.data

import com.walkspot.app.data.local.Converters
import com.walkspot.app.data.local.DailyStepsEntity
import com.walkspot.app.data.local.FindAlternative
import com.walkspot.app.data.local.FindEntity
import com.walkspot.app.data.local.FindKind
import com.walkspot.app.data.local.SyncJson
import com.walkspot.app.data.local.WalkEntity
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class RowJsonTest {

    private val walk = WalkEntity(
        id = "9b2f6c1e-0000-4000-8000-000000000001",
        number = 312,
        startedAt = Instant.parse("2026-09-29T15:40:00Z"),
        endedAt = Instant.parse("2026-09-29T16:28:00Z"),
        durationS = 2880,
        steps = 5230,
        distanceM = 3900,
        route = "27.56 53.9,27.57 53.91",
        updatedAt = Instant.parse("2026-09-29T16:28:05Z"),
    )

    @Test
    fun aWalkGoesUpUnderTheServerColumnNames() {
        val json = SyncJson.encodeToJsonElement(WalkEntity.serializer(), walk).jsonObject

        assertEquals(
            setOf(
                "id", "number", "started_at", "ended_at", "duration_s", "steps", "distance_m",
                "calories", "route", "note", "name", "updated_at",
            ),
            json.keys,
        )
        assertEquals(
            "SRID=4326;LINESTRING(27.56 53.9,27.57 53.91)",
            json.getValue("route").jsonPrimitive.content,
        )
        assertEquals("2026-09-29T15:40:00Z", json.getValue("started_at").jsonPrimitive.content)
    }

    // The row as PostgREST returned it for the walk above, hex route included.
    @Test
    fun aWalkComesBackFromTheServerRow() {
        val row = """
            {"id":"9b2f6c1e-0000-4000-8000-000000000001","user_id":"u","number":312,
             "started_at":"2026-09-29T15:40:00+00:00","ended_at":"2026-09-29T16:28:00+00:00",
             "duration_s":2880,"steps":5230,"distance_m":3900,"calories":0,
             "route":"0102000020E6100000020000008FC2F5285C8F3B403333333333F34A4052B81E85EB913B4014AE47E17AF44A40",
             "note":null,"created_at":"2026-09-29T16:28:06+00:00","updated_at":"2026-09-29T16:28:05+00:00"}
        """.trimIndent()

        val back = SyncJson.decodeFromString(WalkEntity.serializer(), row)

        assertEquals(walk, back)
        assertFalse(back.synced)
    }

    @Test
    fun aDayAndAFindKeepTheirShape() {
        val day = DailyStepsEntity(LocalDate.parse("2026-09-30"), 8200, 6100, 74, 10000, Instant.EPOCH)
        val find = FindEntity(
            id = "f1",
            walkId = walk.id,
            takenAt = Instant.parse("2026-09-29T16:00:00Z"),
            latitude = 53.91,
            longitude = 27.57,
            photoPath = "u/f1.jpg",
            kind = FindKind.PLANT,
            name = "Common yarrow",
            match = 0.94f,
            alternatives = listOf(FindAlternative("Tansy", "Tanacetum vulgare", 0.03f)),
            updatedAt = Instant.EPOCH,
        )

        val dayJson = SyncJson.encodeToString(DailyStepsEntity.serializer(), day)
        val findJson = SyncJson.encodeToString(FindEntity.serializer(), find)

        assertEquals(day, SyncJson.decodeFromString(DailyStepsEntity.serializer(), dayJson))
        assertEquals(find, SyncJson.decodeFromString(FindEntity.serializer(), findJson))
        assertEquals("plant", field(findJson, "kind"))
        assertEquals("2026-09-30", field(dayJson, "day"))
    }

    private fun field(json: String, name: String) =
        SyncJson.parseToJsonElement(json).jsonObject.getValue(name).jsonPrimitive.content

    @Test
    fun alternativesSurviveTheRoomColumn() {
        val converters = Converters()
        val list = listOf(
            FindAlternative("Tansy", "Tanacetum vulgare", 0.03f),
            FindAlternative("Daisy", null, 0.01f),
        )

        assertEquals(list, converters.textToAlternatives(converters.alternativesToText(list)))
    }
}
