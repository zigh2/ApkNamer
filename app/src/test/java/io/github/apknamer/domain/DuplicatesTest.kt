package io.github.apknamer.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicatesTest {
    private fun file(path: String, label: String?, size: Long, hash: String?, pkg: String = "com.a", code: Long = 1) =
        DetectedFile(File(path), PackageType.APK, AppMetadata(label, pkg, "1.0", code), null, size, 0L, hash)

    private fun entry(d: DetectedFile, duplicate: Boolean = true) =
        Entry(d, RenamePlan(d.file, null, PlanStatus.UNCHANGED), InstallState.NOT_INSTALLED, duplicate)

    private val a1 = file("/dl/a1.apk", "App", 100, "h1")
    private val a2 = file("/dl/sub/a2.apk", "App", 100, "h1")
    // same app, same version, different size (e.g. another CPU build)
    private val a3 = file("/dl/a3.apk", "App", 150, "h2")
    // same name and size, different content
    private val a4 = file("/dl/a4.apk", "App", 100, "h3")
    private val b = file("/dl/b.apk", "Other", 100, "h1")

    @Test fun sameVersionButDifferentSizeIsNotADuplicate() {
        assertTrue(Duplicates.key(a1) != Duplicates.key(a3))
    }

    @Test fun sameNameAndSizeButDifferentChecksumIsNotADuplicate() {
        assertTrue(Duplicates.key(a1) != Duplicates.key(a4))
    }

    @Test fun differentAppNamesNeverMatchEvenWithSameChecksum() {
        assertTrue(Duplicates.key(a1) != Duplicates.key(b))
    }

    @Test fun nameSizeAndChecksumMatch() {
        assertEquals(Duplicates.key(a1), Duplicates.key(a2))
    }

    @Test fun noKeyUntilChecksumIsKnown() {
        assertNull(Duplicates.key(file("/dl/x.apk", "App", 100, null)))
    }

    @Test fun onlySameNameAndSizeFilesAreHashCandidates() {
        val c = Duplicates.candidates(listOf(a1, a2, a3, a4, b)).map { it.file.name }.toSet()
        assertEquals(setOf("a1.apk", "a2.apk", "a4.apk"), c)
    }

    @Test fun emptyFilesAreNeverCandidates() {
        val z1 = file("/dl/z1.apk", "App", 0, null)
        val z2 = file("/dl/z2.apk", "App", 0, null)
        assertTrue(Duplicates.candidates(listOf(z1, z2)).isEmpty())
    }

    @Test fun groupsContainOnlyTrueDuplicatesAndSortByWaste() {
        val big1 = file("/dl/big1.apk", "Big", 1000, "hb", pkg = "com.big")
        val big2 = file("/dl/big2.apk", "Big", 1000, "hb", pkg = "com.big")
        val entries = listOf(a1, a2, a3, a4, big1, big2).map { entry(it, duplicate = it !== a3 && it !== a4) }
        val groups = Duplicates.groups(entries)
        assertEquals(2, groups.size)
        assertEquals(1000L, groups[0].wastedBytes)
        assertEquals(setOf("a1.apk", "a2.apk"), groups[1].entries.map { it.detected.file.name }.toSet())
    }

    @Test fun groupOfIncludesSelf() {
        val entries = listOf(a1, a2, a3).map { entry(it) }
        assertEquals(setOf(a1.id, a2.id), Duplicates.groupOf(entries, a1.id).map { it.detected.id }.toSet())
        assertEquals(1, Duplicates.groupOf(entries, a3.id).size)
    }

    @Test fun pruneDropsMissingFiles() {
        val kept = Duplicates.prune(listOf(a1, a2, a3)) { it.name != "a2.apk" }
        assertEquals(listOf(a1.id, a3.id), kept.map { it.id })
    }
}
