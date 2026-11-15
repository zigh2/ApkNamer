package io.github.apknamer.data.scan

import io.github.apknamer.data.parse.ArchiveInspector
import io.github.apknamer.data.parse.ZipMagic
import io.github.apknamer.domain.DetectedFile
import io.github.apknamer.domain.PackageType
import io.github.apknamer.domain.ScanProgress
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Two-phase scan:
 * 1. walk the tree and pick candidates (known extensions, plus any ZIP when [deep] is on);
 * 2. inspect candidates in parallel with a bounded number of workers.
 */
class FileScanner(
    private val inspector: ArchiveInspector,
    private val parallelism: Int = 4,
) {
    suspend fun scan(
        root: File,
        deep: Boolean,
        onProgress: (ScanProgress) -> Unit,
    ): List<DetectedFile> = withContext(Dispatchers.IO) {
        val candidates = ArrayList<Pair<File, Boolean>>()
        var visited = 0

        root.walkTopDown()
            .maxDepth(MAX_DEPTH)
            .onEnter { dir -> dir == root || !isIgnoredDirectory(dir) }
            .forEach { f ->
                ensureActive()
                if (!f.isFile) return@forEach
                visited++
                if (visited % 200 == 0) onProgress(ScanProgress(visited = visited))
                val declared = f.extension.lowercase() in PackageType.KNOWN_EXTENSIONS
                if (declared || (deep && f.length() >= MIN_ZIP_SIZE && ZipMagic.hasZipHeader(f))) {
                    candidates += f to declared
                }
            }

        val total = candidates.size
        val analyzed = AtomicInteger()
        onProgress(ScanProgress(visited = visited, analyzed = 0, total = total))

        val semaphore = Semaphore(parallelism)
        coroutineScope {
            candidates.map { (file, declared) ->
                async {
                    semaphore.withPermit {
                        val result = inspector.inspect(file, declared)
                        onProgress(ScanProgress(visited, analyzed.incrementAndGet(), total))
                        result
                    }
                }
            }.awaitAll()
        }.filterNotNull().sortedBy { it.file.name.lowercase() }
    }

    private fun isIgnoredDirectory(dir: File): Boolean {
        if (dir.name.startsWith(".")) return true
        // Not readable on modern Android even with all-files access.
        return (dir.name == "data" || dir.name == "obb") && dir.parentFile?.name == "Android"
    }

    private companion object {
        const val MAX_DEPTH = 20
        const val MIN_ZIP_SIZE = 22L
    }
}
