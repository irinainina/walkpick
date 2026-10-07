package com.walkspot.app.data

import com.walkspot.app.data.places.PlaceCell
import com.walkspot.app.data.places.PlacesRepository
import com.walkspot.app.data.places.parsePlaces
import com.walkspot.app.data.places.parseSummary
import com.walkspot.app.data.places.placeCell
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

private const val ANSWER = """{"batchcomplete":"","query":{"geosearch":[
    {"pageid":28754198,"ns":0,"title":"Prorizna Street","lat":50.4484,"lon":30.5186,"dist":204.5},
    {"pageid":72475993,"ns":0,"title":"Central Post Office (Kyiv)","lat":50.4499,"lon":30.5232}]}}"""

private const val MONTH_MS = 30L * 24 * 60 * 60 * 1000

class PlacesTest {

    @Test
    fun anAnswerOfWikipediaBecomesPlaces() {
        val places = parsePlaces(ANSWER)

        assertEquals(listOf("Prorizna Street", "Central Post Office (Kyiv)"), places.map { it.title })
        assertEquals(28754198L, places.first().id)
        assertEquals(50.4484, places.first().latitude)
        assertEquals(30.5186, places.first().longitude)
    }

    @Test
    fun theSummaryOfAnArticleGivesItsPictureSmallAndWide() {
        val article = parseSummary(
            """{"title":"Lebedyn","pageid":6862017,"extract":"A city.","thumbnail":{"source":
                "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Tower.jpg/330px-Tower.jpg"}}""",
        )

        assertEquals(6862017L, article?.id)
        assertEquals(true, article?.picture?.endsWith("/330px-Tower.jpg"))
        assertEquals(true, article?.widePicture?.endsWith("/960px-Tower.jpg"))
        assertEquals(null, parseSummary("""{"title":"Nowhere"}""")?.picture)
    }

    @Test
    fun anAnswerThatIsNotOneGivesNoPlaces() {
        assertEquals(emptyList<Any>(), parsePlaces("<html>busy</html>"))
        assertEquals(emptyList<Any>(), parsePlaces("""{"error":{"code":"ratelimited"}}"""))
    }

    @Test
    fun pointsCloseByShareACellAndItsCentreLiesInside() {
        val cell = placeCell(50.4484, 30.5186)

        assertEquals(cell, placeCell(50.4499, 30.5132))
        assertEquals(PlaceCell(2522, 1525), cell)
        assertEquals(50.45, cell.latitude, 1e-9)
        assertEquals(30.51, cell.longitude, 1e-9)
        assertEquals(PlaceCell(-1, -1), placeCell(-0.001, -0.001))
    }

    @Test
    fun aCellIsAskedOnceAndAgainAfterAMonth(@TempDir folder: File) = runTest {
        var asked = 0
        var clock = System.currentTimeMillis()
        val search: suspend (PlaceCell) -> String = {
            asked++
            ANSWER
        }
        val places = PlacesRepository(folder, search, { clock })
        val cell = placeCell(50.4484, 30.5186)

        assertEquals(2, places.near(cell).size)
        assertEquals(2, places.near(cell).size)
        assertEquals(1, asked)

        clock += MONTH_MS * 2
        places.near(cell)
        assertEquals(2, asked)
    }

    @Test
    fun withoutTheNetworkTheKeptPlacesAreShownHoweverOld(@TempDir folder: File) = runTest {
        var online = true
        var clock = System.currentTimeMillis()
        val search: suspend (PlaceCell) -> String = { if (online) ANSWER else throw IOException("offline") }
        val places = PlacesRepository(folder, search, { clock })
        val cell = placeCell(50.4484, 30.5186)
        places.near(cell)

        online = false
        clock += MONTH_MS * 2

        assertEquals(2, places.near(cell).size)
        assertEquals(emptyList<Any>(), places.near(placeCell(48.0, 24.0)))
    }
}
