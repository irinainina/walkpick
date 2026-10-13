package com.walkpick.app.data

import com.walkpick.app.data.account.isEmailAddress
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmailAddressTest {

    @Test
    fun ordinaryAddressesPass() {
        listOf("irina@example.com", "first.last+tag@mail.co.uk")
            .forEach { assertTrue(isEmailAddress(it), it) }
    }

    @Test
    fun brokenAddressesAreCaughtBeforeTheServerIsAsked() {
        listOf("", "irina", "irina@", "irina@example", "@example.com", "ir ina@example.com")
            .forEach { assertFalse(isEmailAddress(it), it) }
    }
}
