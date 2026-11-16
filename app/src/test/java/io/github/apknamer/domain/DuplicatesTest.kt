package io.github.apknamer.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DuplicatesTest {
    private fun file(path: String, pkg: String?, code: Long?) =
        DetectedFile(File(path), PackageType.APK, AppMetadata("App", pkg, "1.0", code), null, 1L, 0L)

    private fun entry(d: DetectedFile) =
        Entry(d, RenamePlan(d.file, null, PlanStatus.UNCHANGED), InstallState.NOT_INSTALLED, false)

    private val a1 = file("/dl/a1.apk", "com.a", 1)
    private val a2 = file("/dl/sub/a2.apk", "com.a", 1)
    private val a3 = file("/dl/a3.apk", "com.a", 2)
    private val b = file("/dl/b.apk", "com.b", 1)
    private val unknown = file("/dl/x.apk", null, null)
    private val entries = listOf(a1, a2, a3, b, unknown).map { entry(it) }

    @Test fun keyNeedsPackageAndCode() {
        assertEquals("com.a|1", Duplicates.key(a1))
        assertNull(Duplicates.key(unknown))
    }

    @Test fun countsGroupsByKey() {
        val counts = Duplicates.counts(listOf(a1, a2, a3, b, unknown))
        assertEquals(2, counts["com.a|1"])
        assertEquals(1, counts["com.a|2"])
    }

    @Test fun groupContainsAllSameVersionFilesIncludingSelf() {
        val group = Duplicates.groupOf(entries, a1.id).map { it.detected.id }
        assertEquals(listOf(a1.id, a2.id).sorted(), group.sorted())
        assertEquals(emptyList<Entry>(), Duplicates.groupOf(entries, unknown.id))
    }

    @Test fun pruneDropsMissingFiles() {
        val kept = Duplicates.prune(listOf(a1, a2, a3)) { it.name != "a2.apk" }
        assertEquals(listOf(a1.id, a3.id), kept.map { it.id })
    }
}
