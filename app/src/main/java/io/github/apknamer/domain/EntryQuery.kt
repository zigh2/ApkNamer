package io.github.apknamer.domain

enum class Filter { ALL, APK, XAPK, APKM, APKS, UPDATES, DUPLICATES }

enum class SortOrder { NAME, APP_NAME, SIZE, DATE, VERSION }

enum class InstallState { NOT_INSTALLED, SAME, UPDATE, DOWNGRADE, UNKNOWN }

/** A detected file together with everything derived from it for display. */
data class Entry(
    val detected: DetectedFile,
    val plan: RenamePlan,
    val install: InstallState,
    val duplicate: Boolean,
)

object InstallStates {
    fun of(fileCode: Long?, installedCode: Long?): InstallState = when {
        installedCode == null -> InstallState.NOT_INSTALLED
        fileCode == null -> InstallState.UNKNOWN
        fileCode > installedCode -> InstallState.UPDATE
        fileCode == installedCode -> InstallState.SAME
        else -> InstallState.DOWNGRADE
    }
}

/** Search, filter and sort, as a pure function so it is easy to test. */
object EntryQuery {
    fun apply(
        entries: List<Entry>,
        query: String,
        filter: Filter,
        sort: SortOrder,
        descending: Boolean,
    ): List<Entry> {
        val q = query.trim()
        val filtered = entries.filter { matchesFilter(it, filter) && (q.isEmpty() || matchesQuery(it, q)) }
        val comparator: Comparator<Entry> = when (sort) {
            SortOrder.NAME -> compareBy<Entry> { it.detected.file.name.lowercase() }
            SortOrder.APP_NAME -> compareBy<Entry> { displayName(it).lowercase() }
            SortOrder.SIZE -> compareBy<Entry> { it.detected.size }
            SortOrder.DATE -> compareBy<Entry> { it.detected.modified }
            SortOrder.VERSION -> compareBy<Entry> { it.detected.metadata?.versionCode ?: -1L }
        }
        val sorted = filtered.sortedWith(comparator)
        return if (descending) sorted.reversed() else sorted
    }

    private fun displayName(e: Entry): String =
        e.detected.metadata?.label ?: e.detected.metadata?.packageName ?: e.detected.file.name

    private fun matchesFilter(e: Entry, filter: Filter): Boolean = when (filter) {
        Filter.ALL -> true
        Filter.APK -> e.detected.type == PackageType.APK
        Filter.XAPK -> e.detected.type == PackageType.XAPK
        Filter.APKM -> e.detected.type == PackageType.APKM
        Filter.APKS -> e.detected.type == PackageType.APKS
        Filter.UPDATES -> e.install == InstallState.UPDATE
        Filter.DUPLICATES -> e.duplicate
    }

    private fun matchesQuery(e: Entry, q: String): Boolean {
        val meta = e.detected.metadata
        return e.detected.file.name.contains(q, ignoreCase = true) ||
            e.plan.target?.name?.contains(q, ignoreCase = true) == true ||
            meta?.label?.contains(q, ignoreCase = true) == true ||
            meta?.packageName?.contains(q, ignoreCase = true) == true
    }
}
