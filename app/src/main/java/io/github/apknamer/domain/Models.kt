package io.github.apknamer.domain

import java.io.File

/** The kind of application package a file was recognised as. */
enum class PackageType(val extension: String, val aliases: Set<String>) {
    APK("apk", setOf("apk")),
    XAPK("xapk", setOf("xapk")),
    APKM("apkm", setOf("apkm")),
    APKS("apks", setOf("apks", "aspk", "apkx")),
    UNKNOWN("", emptySet());

    companion object {
        /** Every extension that is treated as "probably an app package" without looking inside. */
        val KNOWN_EXTENSIONS: Set<String> = entries.flatMap { it.aliases }.toSet()

        fun fromExtension(extension: String): PackageType =
            entries.firstOrNull { extension.lowercase() in it.aliases } ?: UNKNOWN
    }
}

/** Information extracted from a package. Every field is optional because sources differ. */
data class AppMetadata(
    val label: String?,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
) {
    /** At least a name or a package id is needed to build a meaningful file name. */
    val isUsable: Boolean get() = !label.isNullOrBlank() || !packageName.isNullOrBlank()

    fun fillMissingFrom(other: AppMetadata?): AppMetadata =
        if (other == null) this else copy(
            label = label ?: other.label,
            packageName = packageName ?: other.packageName,
            versionName = versionName ?: other.versionName,
            versionCode = versionCode ?: other.versionCode,
        )
}

/** Why a candidate file could not be turned into metadata. */
enum class Problem { NOT_AN_ARCHIVE, CORRUPT, NO_METADATA, NOT_AN_APP }

data class DetectedFile(
    val file: File,
    val type: PackageType,
    val metadata: AppMetadata?,
    val problem: Problem? = null,
) {
    val id: String get() = file.absolutePath
}

enum class PlanStatus { READY, UNCHANGED, UNREADABLE }

data class RenamePlan(val file: File, val target: File?, val status: PlanStatus)

data class RenameOptions(
    val template: String = NameTemplate.DEFAULT,
    val replaceSpaces: Boolean = false,
    val fixExtension: Boolean = true,
)

data class AppSettings(
    val rename: RenameOptions = RenameOptions(),
    val deepScan: Boolean = false,
    val lastRoot: String? = null,
)

data class ScanProgress(val visited: Int = 0, val analyzed: Int = 0, val total: Int = 0)
