package io.github.apknamer.domain

data class CleanupPlan(val keep: List<DetectedFile>, val delete: List<DetectedFile>) {
    val bytes: Long get() = delete.sumOf { it.size }
}

/**
 * Decides which files to remove so that exactly one file per duplicate group remains.
 * Kept in each group: a file that is already named correctly, otherwise the most recently modified one,
 * then the one with the shortest path.
 */
object DuplicateCleanup {
    val EMPTY = CleanupPlan(emptyList(), emptyList())

    fun plan(entries: List<Entry>): CleanupPlan {
        val keep = ArrayList<DetectedFile>()
        val delete = ArrayList<DetectedFile>()
        val groups = entries.filter { it.duplicate }.groupBy { Duplicates.key(it.detected) }
        for ((key, group) in groups) {
            if (key == null || group.size < 2) continue
            val best = group.sortedWith(
                compareByDescending<Entry> { it.plan.status == PlanStatus.UNCHANGED }
                    .thenByDescending { it.detected.modified }
                    .thenBy { it.detected.id.length }
                    .thenBy { it.detected.id },
            ).first()
            for (e in group) if (e === best) keep += e.detected else delete += e.detected
        }
        return CleanupPlan(keep, delete)
    }
}
