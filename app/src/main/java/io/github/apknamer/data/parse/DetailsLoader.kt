package io.github.apknamer.data.parse

import io.github.apknamer.domain.DetectedFile
import io.github.apknamer.domain.PackageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Collects the extra information shown on the details screen (loaded on demand, never during a scan). */
class DetailsLoader(
    private val reader: ApkMetadataReader,
    private val inspector: ArchiveInspector,
    private val installedApps: InstalledApps,
) {
    suspend fun load(item: DetectedFile): FileDetails = withContext(Dispatchers.IO) {
        val packageName = item.metadata?.packageName
        FileDetails(
            apk = inspector.withBaseApk(item.file, packageName) { reader.readDetails(it) },
            contents = if (item.type == PackageType.APK) emptyList() else inspector.bundleEntries(item.file),
            installed = packageName?.let { installedApps.lookup(it) },
        )
    }
}
