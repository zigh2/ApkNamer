package io.github.apknamer.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateGuardTest {
    private fun entry(path: String): Entry {
        val f = File(path)
        val d = DetectedFile(f, PackageType.APK, AppMetadata("App", "com.a", "1", 1), null, 10L, 0L, "h")
        return Entry(d, RenamePlan(f, null, PlanStatus.UNCHANGED), InstallState.NOT_INSTALLED, true)
    }

    private val a = entry("/dl/a.apk")
    private val b = entry("/dl/b.apk")
    private val c = entry("/dl/c.apk")
    private val group = DuplicateGroup("k", listOf(a, b, c))
    private val ids = listOf(a, b, c).map { it.detected.id }

    @Test fun selectingEveryCopyIsAViolation() {
        assertEquals(1, DuplicateGuard.violations(listOf(group), ids.toSet()).size)
    }

    @Test fun leavingOneCopyIsAllowed() {
        assertTrue(DuplicateGuard.violations(listOf(group), setOf(ids[0], ids[1])).isEmpty())
    }

    @Test fun lastUnselectedCopyCannotBeSelected() {
        assertTrue(DuplicateGuard.wouldRemoveLastCopy(listOf(group), setOf(ids[0], ids[1]), ids[2]))
        assertFalse(DuplicateGuard.wouldRemoveLastCopy(listOf(group), setOf(ids[0]), ids[1]))
    }
}
