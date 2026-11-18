package io.github.apknamer.data.scan

import io.github.apknamer.data.parse.FileHasher
import io.github.apknamer.domain.DetectedFile
import io.github.apknamer.domain.Duplicates
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Step 3 of duplicate detection. Only files that already match on app name and size are read in full,
 * and results are cached by path, size and modification time so rescans are instant.
 */
class DuplicateHasher(private val parallelism: Int = 2) {
    private val cache = ConcurrentHashMap<String, String>()

    suspend fun fill(files: List<DetectedFile>, onProgress: (done: Int, total: Int) -> Unit): List<DetectedFile> =
        withContext(Dispatchers.IO) {
            val targets = Duplicates.candidates(files)
            val total = targets.size
            val done = AtomicInteger()
            val gate = Semaphore(parallelism)
            onProgress(0, total)
            val hashes: Map<String, String?> = coroutineScope {
                targets.map { file ->
                    async {
                        gate.withPermit {
                            val key = "${file.id}|${file.size}|${file.modified}"
                            val hash = cache[key]
                                ?: runCatching { FileHasher.sha256(file.file) }.getOrNull()?.also { cache[key] = it }
                            onProgress(done.incrementAndGet(), total)
                            file.id to hash
                        }
                    }
                }.awaitAll()
            }.toMap()
            files.map { f -> hashes[f.id]?.let { f.copy(hash = it) } ?: f }
        }
}
