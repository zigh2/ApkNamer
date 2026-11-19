package io.github.apknamer.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryQueryTest {
    private fun entry(
        name: String,
        label: String,
        pkg: String,
        code: Long,
        type: PackageType = PackageType.APK,
        size: Long = 0,
        install: InstallState = InstallState.NOT_INSTALLED,
        duplicate: Boolean = false,
        status: PlanStatus = PlanStatus.UNCHANGED,
    ): Entry {
        val file = File("/dl/$name")
        val detected = DetectedFile(file, type, AppMetadata(label, pkg, "1.0", code), null, size, 0L)
        return Entry(detected, RenamePlan(file, null, status), install, duplicate)
    }

    private val a = entry("b.apk", "Zeta", "com.z", 3, size = 10)
    private val b = entry("a.xapk", "Alpha", "com.a", 1, PackageType.XAPK, size = 30, install = InstallState.UPDATE)
    private val c = entry("c.apk", "Mid", "com.m", 2, size = 20, duplicate = true)
    private val all = listOf(a, b, c)

    private fun names(r: List<Entry>) = r.map { it.detected.file.name }

    @Test fun sortsByFileName() {
        assertEquals(listOf("a.xapk", "b.apk", "c.apk"), names(EntryQuery.apply(all, "", Filter.ALL, SortOrder.NAME, false)))
    }

    @Test fun sortsByAppNameDescending() {
        assertEquals(listOf("b.apk", "c.apk", "a.xapk"), names(EntryQuery.apply(all, "", Filter.ALL, SortOrder.APP_NAME, true)))
    }

    @Test fun sortsBySizeAndVersion() {
        assertEquals(listOf("b.apk", "c.apk", "a.xapk"), names(EntryQuery.apply(all, "", Filter.ALL, SortOrder.SIZE, false)))
        assertEquals(listOf("a.xapk", "c.apk", "b.apk"), names(EntryQuery.apply(all, "", Filter.ALL, SortOrder.VERSION, false)))
    }

    @Test fun filtersByTypeUpdatesAndDuplicates() {
        assertEquals(listOf("a.xapk"), names(EntryQuery.apply(all, "", Filter.XAPK, SortOrder.NAME, false)))
        assertEquals(listOf("a.xapk"), names(EntryQuery.apply(all, "", Filter.UPDATES, SortOrder.NAME, false)))
        assertEquals(listOf("c.apk"), names(EntryQuery.apply(all, "", Filter.DUPLICATES, SortOrder.NAME, false)))
    }

    @Test fun searchMatchesLabelPackageAndFileName() {
        assertEquals(listOf("a.xapk"), names(EntryQuery.apply(all, "alp", Filter.ALL, SortOrder.NAME, false)))
        assertEquals(listOf("c.apk"), names(EntryQuery.apply(all, "COM.M", Filter.ALL, SortOrder.NAME, false)))
        assertEquals(listOf("b.apk"), names(EntryQuery.apply(all, "b.apk", Filter.ALL, SortOrder.NAME, false)))
    }

    @Test fun needsRenameAndNamedFilters() {
        val todo = entry("t.apk", "Todo", "com.t", 1, status = PlanStatus.READY)
        val done = entry("d.apk", "Done", "com.d", 1, status = PlanStatus.UNCHANGED)
        val broken = entry("x.apk", "Broken", "com.x", 1, status = PlanStatus.UNREADABLE)
        val list = listOf(todo, done, broken)
        assertEquals(listOf("t.apk"), names(EntryQuery.apply(list, "", Filter.NEEDS_RENAME, SortOrder.NAME, false)))
        assertEquals(listOf("d.apk"), names(EntryQuery.apply(list, "", Filter.NAMED, SortOrder.NAME, false)))
        assertEquals(3, EntryQuery.apply(list, "", Filter.ALL, SortOrder.NAME, false).size)
    }

    @Test fun installStates() {
        assertEquals(InstallState.NOT_INSTALLED, InstallStates.of(5, null))
        assertEquals(InstallState.UPDATE, InstallStates.of(6, 5))
        assertEquals(InstallState.SAME, InstallStates.of(5, 5))
        assertEquals(InstallState.DOWNGRADE, InstallStates.of(4, 5))
        assertEquals(InstallState.UNKNOWN, InstallStates.of(null, 5))
    }
}
