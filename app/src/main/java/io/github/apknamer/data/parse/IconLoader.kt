package io.github.apknamer.data.parse

import android.graphics.Bitmap
import android.util.LruCache
import io.github.apknamer.domain.DetectedFile
import io.github.apknamer.domain.PackageType
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** Loads app icons lazily (only for rows that are on screen) and keeps them in a memory-bounded cache. */
class IconLoader(
    private val inspector: ArchiveInspector,
    private val reader: ApkMetadataReader,
) {
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 16).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val missing = ConcurrentHashMap.newKeySet<String>()
    private val gate = Semaphore(2)

    fun cached(item: DetectedFile): Bitmap? = cache.get(key(item))

    suspend fun load(item: DetectedFile): Bitmap? {
        val key = key(item)
        cache.get(key)?.let { return it }
        if (key in missing) return null
        return gate.withPermit {
            withContext(Dispatchers.IO) {
                val bitmap = (if (item.type != PackageType.APK) inspector.readEmbeddedIcon(item.file, SIZE) else null)
                    ?: inspector.withBaseApk(item.file, item.metadata?.packageName) { reader.loadIcon(it, SIZE) }
                if (bitmap != null) cache.put(key, bitmap) else missing.add(key)
                bitmap
            }
        }
    }

    private fun key(item: DetectedFile) = "${item.file.absolutePath}|${item.size}|${item.modified}"

    private companion object {
        const val SIZE = 96
    }
}
