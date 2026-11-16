package io.github.apknamer.data.parse

import io.github.apknamer.domain.AppMetadata
import io.github.apknamer.domain.DetectedFile
import io.github.apknamer.domain.PackageType
import io.github.apknamer.domain.Problem
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Looks inside a file and decides whether it is an app package, and of which kind:
 *
 * | Detected by                                   | Type  |
 * |-----------------------------------------------|-------|
 * | `AndroidManifest.xml` at the archive root     | APK   |
 * | `manifest.json` with `package_name`           | XAPK  |
 * | `info.json` with `pkg_name`                   | APKM  |
 * | nested `*.apk` entries (base + splits)        | APKS  |
 */
class ArchiveInspector(
    private val reader: ApkMetadataReader,
    private val cacheDir: File,
) {
    /**
     * @param declared true when the file extension already suggests an app package; such files are
     * reported (with a [Problem]) even if they turn out to be unreadable. Other files are only
     * reported when they positively look like packages.
     */
    fun inspect(file: File, declared: Boolean): DetectedFile? {
        val extType = PackageType.fromExtension(file.extension)
        if (!ZipMagic.hasZipHeader(file)) {
            return if (declared) DetectedFile(file, extType, null, Problem.NOT_AN_ARCHIVE) else null
        }
        val zip = try {
            ZipFile(file)
        } catch (_: Exception) {
            return if (declared) DetectedFile(file, extType, null, Problem.CORRUPT) else null
        }
        return try {
            zip.use { inspectZip(file, it, declared) }
        } catch (_: Exception) {
            if (declared) DetectedFile(file, extType, null, Problem.CORRUPT) else null
        }
    }

    private fun inspectZip(file: File, zip: ZipFile, declared: Boolean): DetectedFile? {
        val extType = PackageType.fromExtension(file.extension)

        if (zip.getEntry("AndroidManifest.xml") != null) {
            return result(file, PackageType.APK, reader.read(file))
        }

        readText(zip, "manifest.json")?.let(BundleManifest::parseXapk)?.let { meta ->
            return result(file, PackageType.XAPK, enrich(zip, meta))
        }
        readText(zip, "info.json")?.let(BundleManifest::parseApkm)?.let { meta ->
            return result(file, PackageType.APKM, enrich(zip, meta))
        }

        val apks = apkEntries(zip)
        if (apks.isNotEmpty()) {
            val looksLikeBundle = declared || apks.size >= 2 || apks.any { baseName(it) in BASE_NAMES }
            if (looksLikeBundle) {
                val type = extType.takeIf { it != PackageType.APK && it != PackageType.UNKNOWN }
                    ?: PackageType.APKS
                return result(file, type, extractAndRead(zip, pickBase(apks, null)))
            }
        }

        return if (declared) DetectedFile(file, extType, null, Problem.NOT_AN_APP) else null
    }

    private fun result(file: File, type: PackageType, meta: AppMetadata?) =
        DetectedFile(file, type, meta, if (meta == null) Problem.NO_METADATA else null)

    /** Fills gaps in the JSON descriptor from the bundle's base APK (only extracted when needed). */
    private fun enrich(zip: ZipFile, meta: AppMetadata): AppMetadata {
        if (meta.label != null && meta.versionName != null && meta.versionCode != null) return meta
        val base = extractAndRead(zip, pickBase(apkEntries(zip), meta.packageName))
        return meta.fillMissingFrom(base)
    }

    private fun apkEntries(zip: ZipFile): List<ZipEntry> =
        zip.entries().asSequence()
            .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
            .toList()

    private fun baseName(entry: ZipEntry) = entry.name.substringAfterLast('/').lowercase()

    private fun pickBase(entries: List<ZipEntry>, packageName: String?): ZipEntry? {
        if (entries.isEmpty()) return null
        val preferred = buildSet {
            addAll(BASE_NAMES)
            if (packageName != null) add("${packageName.lowercase()}.apk")
        }
        return entries.firstOrNull { baseName(it) in preferred }
            ?: entries.filterNot { isSplitConfig(baseName(it)) }.maxByOrNull { it.size }
            ?: entries.maxByOrNull { it.size }
    }

    private fun isSplitConfig(name: String) =
        name.startsWith("config.") || name.startsWith("split_config.") || name.startsWith("split_")

    private fun readText(zip: ZipFile, name: String): String? {
        val entry = zip.getEntry(name) ?: return null
        if (entry.isDirectory || entry.size > MAX_JSON_BYTES) return null
        return try {
            zip.getInputStream(entry).bufferedReader().use { it.readText() }
        } catch (_: IOException) {
            null
        }
    }

    private fun extractAndRead(zip: ZipFile, entry: ZipEntry?): AppMetadata? {
        if (entry == null) return null
        var tmp: File? = null
        return try {
            cacheDir.mkdirs()
            val created = File.createTempFile("base_", ".apk", cacheDir)
            tmp = created
            zip.getInputStream(entry).use { input -> created.outputStream().use { input.copyTo(it) } }
            reader.read(created)
        } catch (_: Exception) {
            null
        } finally {
            tmp?.delete()
        }
    }

    private companion object {
        const val MAX_JSON_BYTES = 1L * 1024 * 1024
        val BASE_NAMES = setOf("base.apk", "base-master.apk", "standalone.apk")
    }
}
