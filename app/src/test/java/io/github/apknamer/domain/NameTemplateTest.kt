package io.github.apknamer.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NameTemplateTest {
    private val full: Map<String, String?> = mapOf(
        "name" to "Telegram", "package" to "org.telegram.messenger",
        "version" to "10.14.5", "code" to "5034", "type" to "APK", "original" to "download",
        "minandroid" to "8.0", "targetandroid" to "14", "arch" to "v8",
    )
    private val legacy = NameTemplate.LEGACY_DEFAULT

    @Test fun legacyTemplateWithAllValues() {
        assertEquals("Telegram v10.14.5 (5034)", NameTemplate.render(legacy, full))
    }

    @Test fun newDefaultShowsVersionAndroidRangeAndCpu() {
        assertEquals(
            "Telegram V10.14.5 (Android 8.0 to 14) v8a",
            NameTemplate.render(NameTemplate.DEFAULT, full),
        )
    }

    @Test fun newDefaultDropsGroupsWhoseValuesAreMissing() {
        assertEquals("Telegram V10.14.5 v8a", NameTemplate.render(NameTemplate.DEFAULT, full + ("targetandroid" to null)))
        assertEquals("Telegram V10.14.5 (Android 8.0 to 14)", NameTemplate.render(NameTemplate.DEFAULT, full + ("arch" to null)))
        assertEquals("Telegram", NameTemplate.render(NameTemplate.DEFAULT, mapOf("name" to "Telegram")))
    }

    @Test fun optionalGroupIsDroppedWhenValueMissing() {
        assertEquals("Telegram (5034)", NameTemplate.render(legacy, full + ("version" to null)))
        assertEquals("Telegram v10.14.5", NameTemplate.render(legacy, full + ("code" to null)))
        assertEquals("Telegram", NameTemplate.render(legacy, full + ("version" to null) + ("code" to "")))
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

    @Test fun everyDefaultTokenIsKnown() {
        assertEquals(emptyList<String>(), NameTemplate.unknownTokens(NameTemplate.DEFAULT))
    }
}
