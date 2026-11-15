package io.github.apknamer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNameSanitizerTest {
    @Test fun replacesIllegalCharacters() {
        assertEquals("a_b_c", FileNameSanitizer.sanitize("a/b:c"))
    }

    @Test fun collapsesWhitespaceAndTrimsDots() {
        assertEquals("My App", FileNameSanitizer.sanitize("  ..My    App.. "))
    }

    @Test fun canReplaceSpaces() {
        assertEquals("My_App", FileNameSanitizer.sanitize("My App", replaceSpaces = true))
    }

    @Test fun keepsNonLatinNames() {
        assertEquals("تطبيق 1.0", FileNameSanitizer.sanitize("تطبيق 1.0"))
    }

    @Test fun truncatesByUtf8Bytes() {
        val result = FileNameSanitizer.sanitize("ع".repeat(300), maxBytes = 20)
        assertTrue(result.toByteArray(Charsets.UTF_8).size <= 20)
        assertTrue(result.isNotEmpty())
    }
}
