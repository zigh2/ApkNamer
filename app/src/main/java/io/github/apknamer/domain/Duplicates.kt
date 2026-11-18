package io.github.apknamer.domain

import java.io.File

/** Several byte-identical copies of the same app file. */
data class DuplicateGroup(val key: String, val entries: List<Entry>) {
    val size: Long get() = entries.first().detected.size
    val wastedBytes: Long get() = size * (entries.size - 1)
}

/**
 * Files are duplicates only when they match, in this order, on:
 * 1. app name, 2. file size, 3. a checksum of the whole file.
 * Same package and version is NOT enough: builds for different CPUs or with different contents differ in size or checksum.
 */
object Duplicates {
    fun identity(d: DetectedFile): String =
        (d.metadata?.label ?: d.metadata?.packageName ?: "").trim().lowercase()

    private fun candidateKey(d: DetectedFile) = identity(d) + "|" + d.size

    /** Files that share app name and size with another file; only these need a checksum. */
    fun candidates(files: List<DetectedFile>): List<DetectedFile> {
        val counts = files.filter { it.size > 0 }.groupingBy { candidateKey(it) }.eachCount()
        return files.filter { it.size > 0 && (counts[candidateKey(it)] ?: 0) > 1 }
    }

    /** Verified duplicate key (name + size + checksum); null while the checksum is unknown. */
    fun key(d: DetectedFile): String? = d.hash?.let { candidateKey(d) + "|" + it }

    fun counts(files: List<DetectedFile>): Map<String, Int> =
        files.mapNotNull { key(it) }.groupingBy { it }.eachCount()

    /** Every entry in the same duplicate group as [id], including [id] itself; empty when it has no key. */
    fun groupOf(entries: List<Entry>, id: String): List<Entry> {
        val key = entries.firstOrNull { it.detected.id == id }?.let { key(it.detected) } ?: return emptyList()
        return entries.filter { key(it.detected) == key }.sortedBy { it.detected.id.lowercase() }
    }

    /** All duplicate groups, the ones wasting the most space first. */
    fun groups(entries: List<Entry>): List<DuplicateGroup> =
        entries.filter { it.duplicate }
            .groupBy { key(it.detected) }
            .mapNotNull { (key, group) ->
                if (key != null && group.size > 1) {
                    DuplicateGroup(key, group.sortedBy { it.detected.id.lowercase() })
                } else {
                    null
                }
            }
            .sortedByDescending { it.wastedBytes }

    /** Drops files that no longer exist (used to re-verify everything after a deletion). */
    fun prune(files: List<DetectedFile>, exists: (File) -> Boolean): List<DetectedFile> =
        files.filter { exists(it.file) }
}

/** Safety rule: at least one copy of every app must remain. */
object DuplicateGuard {
    /** Groups in which every copy is selected, i.e. where deleting would remove the app entirely. */
    fun violations(groups: List<DuplicateGroup>, selected: Set<String>): List<DuplicateGroup> =
        groups.filter { g -> g.entries.all { it.detected.id in selected } }

    /** True if selecting [id] would leave its group without any unselected copy. */
    fun wouldRemoveLastCopy(groups: List<DuplicateGroup>, selected: Set<String>, id: String): Boolean {
        val group = groups.firstOrNull { g -> g.entries.any { it.detected.id == id } } ?: return false
        return group.entries.filter { it.detected.id != id }.all { it.detected.id in selected }
    }
}
