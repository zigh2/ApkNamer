package io.github.apknamer.data.parse

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import io.github.apknamer.domain.AppMetadata
import java.io.File

/** Reads name / package / version from a plain APK using the system package parser. */
class ApkMetadataReader(context: Context) {
    private val pm: PackageManager = context.applicationContext.packageManager

    fun read(apk: File): AppMetadata? {
        val path = apk.absolutePath
        @Suppress("DEPRECATION")
        val info = runCatching { pm.getPackageArchiveInfo(path, 0) }.getOrNull() ?: return null
        val packageName = info.packageName ?: return null

        // Needed so that loadLabel() resolves resources from this archive.
        val label = info.applicationInfo?.let { ai ->
            ai.sourceDir = path
            ai.publicSourceDir = path
            runCatching { ai.loadLabel(pm).toString() }.getOrNull()
        }

        return AppMetadata(
            label = label?.trim()?.takeIf { it.isNotEmpty() && it != packageName },
            packageName = packageName,
            versionName = info.versionName?.trim()?.takeIf { it.isNotEmpty() },
            versionCode = runCatching { PackageInfoCompat.getLongVersionCode(info) }.getOrNull(),
        )
    }
}
