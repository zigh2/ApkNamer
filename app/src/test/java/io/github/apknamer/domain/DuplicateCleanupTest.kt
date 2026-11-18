package io.github.apknamer.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateCleanupTest {
    private fun entry(
        path: String,
        pkg: String,
        code: Long,
        modified: Long,
        size: Long,
        unchanged: Boolean = false,
        duplicate: Boolean = true,
    ): Entry {
        val file = File(path)
        val detected = DetectedFile(file, PackageType.APK, AppMetadata("App", pkg, "1.0", code), null, size, modified, "hash-$pkg")
        val status = if (unchanged) PlanStatus.UNCHANGED else PlanStatus.READY
        return Entry(detected, RenamePlan(file, null, status), InstallState.NOT_INSTALLED, duplicate)
    }

    @Test fun keepsNewestAndDeletesTheRest() {
        val plan = DuplicateCleanup.plan(
            listOf(
                entry("/dl/a.apk", "com.a", 1, modified = 10, size = 100),
                entry("/dl/b.apk", "com.a", 1, modified = 30, size = 100),
                entry("/dl/c.apk", "com.a", 1, modified = 20, size = 100),
            ),
        )
        assertEquals(listOf(File("/dl/b.apk").absolutePath), plan.keep.map { it.id })
        assertEquals(
            setOf(File("/dl/a.apk").absolutePath, File("/dl/c.apk").absolutePath),
            plan.delete.map { it.id }.toSet(),
        )
        assertEquals(200L, plan.bytes)
    }

    @Test fun prefersAlreadyCorrectlyNamedFile() {
        val plan = DuplicateCleanup.plan(
            listOf(
                entry("/dl/new.apk", "com.a", 1, modified = 99, size = 1),
                entry("/dl/Good Name.apk", "com.a", 1, modified = 1, size = 1, unchanged = true),
            ),
        )
        assertEquals(listOf(File("/dl/Good Name.apk").absolutePath), plan.keep.map { it.id })
    }

    @Test fun groupsAreIndependentAndNonDuplicatesAreIgnored() {
        val plan = DuplicateCleanup.plan(
            listOf(
                entry("/dl/a1.apk", "com.a", 1, 1, 1),
                entry("/dl/a2.apk", "com.a", 1, 2, 1),
                entry("/dl/b1.apk", "com.b", 1, 1, 1),
                entry("/dl/b2.apk", "com.b", 1, 2, 1),
                entry("/dl/solo.apk", "com.c", 1, 1, 1, duplicate = false),
            ),
        )
        assertEquals(2, plan.keep.size)
        assertEquals(2, plan.delete.size)
        assertTrue(plan.delete.none { it.file.name == "solo.apk" })
    }

    @Test fun emptyWhenNothingIsDuplicated() {
        assertEquals(0, DuplicateCleanup.plan(emptyList()).delete.size)
    }
}
