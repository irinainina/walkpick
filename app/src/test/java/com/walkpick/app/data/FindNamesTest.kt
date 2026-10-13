package com.walkpick.app.data

import com.walkpick.app.data.find.asKind
import com.walkpick.app.data.find.choosing
import com.walkpick.app.data.find.options
import com.walkpick.app.data.find.renamed
import com.walkpick.app.data.local.FindAlternative
import com.walkpick.app.data.local.FindEntity
import com.walkpick.app.data.local.FindKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.time.Instant

class FindNamesTest {

    private val noon = Instant.parse("2026-10-07T12:00:00Z")

    private val yarrow = FindEntity(
        id = "find",
        walkId = "walk",
        takenAt = noon,
        latitude = 50.45,
        longitude = 30.52,
        photoPath = "user/find.jpg",
        kind = FindKind.PLANT,
        name = "Common yarrow",
        latinName = "Achillea millefolium",
        match = 0.94f,
        alternatives = listOf(
            FindAlternative("Sneezewort", "Achillea ptarmica", 0.04f),
            FindAlternative("Oxeye daisy", "Leucanthemum vulgare", 0.02f),
        ),
        description = "A hardy wildflower.",
        wikipediaUrl = "https://en.wikipedia.org/wiki/Achillea_millefolium",
        updatedAt = noon,
    )

    @Test
    fun theNameAFindHasComesFirstAmongItsOptions() {
        assertEquals(listOf("Common yarrow", "Sneezewort", "Oxeye daisy"), yarrow.options().map { it.name })
        assertEquals(0.94f, yarrow.options().first().match)
    }

    @Test
    fun anotherOptionTakesTheNameAndTheOldNameGoesBackAmongTheOthers() {
        val changed = yarrow.choosing(1)

        assertEquals("Sneezewort", changed.name)
        assertEquals("Achillea ptarmica", changed.latinName)
        assertEquals(0.04f, changed.match)
        assertEquals(listOf("Common yarrow", "Oxeye daisy"), changed.alternatives.map { it.name })
        assertNull(changed.description)
        assertNull(changed.wikipediaUrl)
    }

    @Test
    fun pickingTheNameItHasOrNoOptionAtAllChangesNothing() {
        assertSame(yarrow, yarrow.choosing(0))
        assertSame(yarrow, yarrow.choosing(7))
    }

    @Test
    fun aNameTypedByHandHasNoMatchAndKeepsTheGuessesForAChangeOfMind() {
        val named = yarrow.renamed("  Milfoil ")

        assertEquals("Milfoil", named.name)
        assertNull(named.latinName)
        assertNull(named.match)
        assertEquals(listOf("Common yarrow", "Sneezewort", "Oxeye daisy"), named.alternatives.map { it.name })
        assertNull(named.description)
        assertEquals("Common yarrow" to 0.94f, named.choosing(1).let { it.name to it.match })
    }

    @Test
    fun aBlankNameOrTheSameNameChangesNothing() {
        assertSame(yarrow, yarrow.renamed("   "))
        assertSame(yarrow, yarrow.renamed("Common yarrow"))
    }

    @Test
    fun aPlaceTakesAnotherArticleWithoutAMatch() {
        val church = yarrow.copy(
            kind = FindKind.PLACE,
            name = "St. Nicholas Church",
            latinName = null,
            match = null,
            alternatives = listOf(FindAlternative("Old Town Hall", null, 0f)),
        )

        assertEquals("Old Town Hall" to null, church.choosing(1).let { it.name to it.match })
    }

    @Test
    fun `a find said to be of another kind loses what the old service found`() {
        val bird = yarrow.asKind(FindKind.BIRD)
        assertEquals(FindKind.BIRD, bird.kind)
        assertNull(bird.name)
        assertNull(bird.latinName)
        assertNull(bird.match)
        assertEquals(emptyList<FindAlternative>(), bird.alternatives)
    }

    @Test
    fun `a find said to be what it already is stays as it was`() {
        assertSame(yarrow, yarrow.asKind(FindKind.PLANT))
    }
}
