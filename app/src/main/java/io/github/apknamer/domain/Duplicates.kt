package io.github.apknamer.domain

import java.io.File

/** Duplicates are files of the same package and the same version code. */
object Duplicates {
    fun key(d: DetectedFile): String? {
        val pkg = d.metadata?.packageName
        val code = d.metadata?.versionCode
        return if (pkg != null && code != null) "$pkg|$code" else null
    }

    fun counts(files: List<DetectedFile>): Map<String, Int> =
        files.mapNotNull { key(it) }.groupingBy { it }.eachCount()

    /** Every entry in the same duplicate group as [id], including [id] itself; empty when it has no key. */
    fun groupOf(entries: List<Entry>, id: String): List<Entry> {
        val key = entries.firstOrNull { it.detected.id == id }?.let { key(it.detected) } ?: return emptyList()
        return entries.filter { key(it.detected) == key }.sortedBy { it.detected.id.lowercase() }
    }

    /** Drops files that no longer exist (used to re-verify everything after a deletion). */
    fun prune(files: List<DetectedFile>, exists: (File) -> Boolean): List<DetectedFile> =
        files.filter { exists(it.file) }
}
