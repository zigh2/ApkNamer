package io.github.apknamer.data.rename

import io.github.apknamer.domain.PlanStatus
import io.github.apknamer.domain.RenamePlan
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** [records] describe what actually moved: `from` is the path before, `to` the path after. */
data class RenameReport(val records: List<RenameRecord>, val failed: Int)

class Renamer(private val history: RenameHistory) {

    suspend fun canUndo(): Boolean = history.hasBatches()

    suspend fun apply(plans: List<RenamePlan>): RenameReport =
        withContext(Dispatchers.IO + NonCancellable) {
            val done = ArrayList<RenameRecord>()
            var failed = 0
            for (plan in plans) {
                val target = plan.target
                if (plan.status != PlanStatus.READY || target == null) continue
                if (!target.exists() && plan.file.renameTo(target)) {
                    done += RenameRecord(plan.file.absolutePath, target.absolutePath)
                } else {
                    failed++
                }
            }
            if (done.isNotEmpty()) history.push(done)
            RenameReport(done, failed)
        }

    /** Reverts the latest batch. Returns null when there is nothing to undo. */
    suspend fun undoLast(): RenameReport? =
        withContext(Dispatchers.IO + NonCancellable) {
            val batch = history.pop() ?: return@withContext null
            val restored = ArrayList<RenameRecord>()
            var failed = 0
            for (record in batch.asReversed()) {
                val current = File(record.to)
                val original = File(record.from)
                if (current.exists() && !original.exists() && current.renameTo(original)) {
                    restored += RenameRecord(record.to, record.from)
                } else {
                    failed++
                }
            }
            RenameReport(restored, failed)
        }
}
