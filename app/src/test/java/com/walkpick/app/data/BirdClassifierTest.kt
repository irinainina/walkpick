package com.walkpick.app.data

import com.walkpick.app.data.find.birdGuesses
import com.walkpick.app.data.find.birdLabels
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BirdClassifierTest {

    private val labels = birdLabels("id,name\n3,background\n0,Parus major\n1,Pica pica\n2,Turdus merula\n")

    @Test
    fun theLabelFileGivesNamesByNumberAndItsHeadingIsSkipped() {
        assertEquals(4, labels.size)
        assertEquals("Parus major", labels[0])
        assertEquals("background", labels[3])
    }

    @Test
    fun theLikeliestBirdComesFirstAndNoBirdIsNeverAGuess() {
        val guesses = birdGuesses(floatArrayOf(0.1f, 0.7f, 0.004f, 0.15f), labels)

        assertEquals(listOf("Pica pica", "Parus major"), guesses.map { it.name })
        assertEquals("Pica pica", guesses.first().latinName)
        assertEquals(0.7f, guesses.first().match)
    }

    @Test
    fun aPhotoWithoutABirdOrAnUnsureGuessGivesNothing() {
        assertEquals(emptyList<Any>(), birdGuesses(floatArrayOf(0.2f, 0.1f, 0.1f, 0.6f), labels))
        assertEquals(emptyList<Any>(), birdGuesses(floatArrayOf(0.15f, 0.1f, 0.1f, 0.05f), labels))
    }
}
