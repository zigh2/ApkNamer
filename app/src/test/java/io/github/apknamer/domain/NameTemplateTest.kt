package io.github.apknamer.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NameTemplateTest {
    private val full: Map<String, String?> = mapOf(
        "name" to "Telegram", "package" to "org.telegram.messenger",
        "version" to "10.14.5", "code" to "5034", "type" to "APK", "original" to "download",
    )

    @Test fun defaultTemplateWithAllValues() {
        assertEquals("Telegram v10.14.5 (5034)", NameTemplate.render(NameTemplate.DEFAULT, full))
    }

    @Test fun optionalGroupIsDroppedWhenValueMissing() {
        assertEquals("Telegram (5034)", NameTemplate.render(NameTemplate.DEFAULT, full + ("version" to null)))
        assertEquals("Telegram v10.14.5", NameTemplate.render(NameTemplate.DEFAULT, full + ("code" to null)))
        assertEquals("Telegram", NameTemplate.render(NameTemplate.DEFAULT, full + ("version" to null) + ("code" to "")))
    }

    @Test fun emptyBracketsAreRemoved() {
        assertEquals("Telegram", NameTemplate.render("{name} ({version})", full + ("version" to null)))
    }

    @Test fun packageTemplate() {
        assertEquals("org.telegram.messenger-10.14.5-5034", NameTemplate.render("{package}-{version}-{code}", full))
    }

    @Test fun unknownTokensAreReported() {
        assertEquals(listOf("foo"), NameTemplate.unknownTokens("{name}{foo}{foo}"))
    }
}
