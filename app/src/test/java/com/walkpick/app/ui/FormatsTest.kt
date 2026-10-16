package com.walkpick.app.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormatsTest {

    @Test
    fun thousandsAreSplitByANoBreakSpace() {
        assertEquals("540", grouped(540))
        assertEquals("1\u00A0209", grouped(1209))
        assertEquals("1\u00A0209\u00A0000", grouped(1_209_000))
    }
}
