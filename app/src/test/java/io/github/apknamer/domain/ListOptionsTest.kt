package io.github.apknamer.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListOptionsTest {
    @Test fun presetsDiffer() {
        assertNotEquals(ListOptions.COMPACT, ListOptions.STANDARD)
        assertNotEquals(ListOptions.STANDARD, ListOptions.DETAILED)
    }

    @Test fun compactHidesSecondaryFields() {
        val c = ListOptions.COMPACT
        assertFalse(c.size || c.date || c.path || c.oldName || c.versionCode)
        assertTrue(c.icons && c.versionName && c.typeTag)
    }

    @Test fun detailedAddsPackage() {
        assertTrue(ListOptions.DETAILED.packageName)
        assertFalse(ListOptions.STANDARD.packageName)
    }
}
