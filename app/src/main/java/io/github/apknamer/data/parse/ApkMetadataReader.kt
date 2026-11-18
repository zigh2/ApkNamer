package io.github.apknamer.data.parse

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import io.github.apknamer.domain.AppMetadata
import java.io.File
import java.security.MessageDigest

/** Reads name / package / version / icon / details from a plain APK using the system package parser. */
class ApkMetadataReader(context: Context) {
    private val pm: PackageManager = context.applicationContext.packageManager

    @Suppress("DEPRECATION")
    private fun archive(path: String, flags: Int): PackageInfo? =
        runCatching { pm.getPackageArchiveInfo(path, flags) }.getOrNull()

    fun read(apk: File): AppMetadata? {
        val path = apk.absolutePath
        val info = archive(path, 0) ?: return null
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
            minSdk = info.applicationInfo?.minSdkVersion?.takeIf { it > 0 },
            targetSdk = info.applicationInfo?.targetSdkVersion?.takeIf { it > 0 },
        )
    }

    /** The launcher icon rendered to a square bitmap, or null when the app declares none. */
    fun loadIcon(apk: File, sizePx: Int): Bitmap? {
        val path = apk.absolutePath
        val ai = archive(path, 0)?.applicationInfo ?: return null
        if (ai.icon == 0) return null
        ai.sourceDir = path
        ai.publicSourceDir = path
        return runCatching { ai.loadIcon(pm).toBitmap(sizePx, sizePx) }.getOrNull()
    }

    fun readDetails(apk: File): ApkDetails? {
        val path = apk.absolutePath
        val signingFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        }
        val info = archive(path, PackageManager.GET_PERMISSIONS or signingFlag)
            ?: archive(path, PackageManager.GET_PERMISSIONS)
            ?: return null
        val ai = info.applicationInfo
        return ApkDetails(
            minSdk = ai?.minSdkVersion,
            targetSdk = ai?.targetSdkVersion,
            permissions = info.requestedPermissions?.toList().orEmpty(),
            signerSha256 = signerSha256(info),
        )
    }

    private fun signerSha256(info: PackageInfo): String? {
        val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION") info.signatures
        }
        val first = signatures?.firstOrNull() ?: return null
        val digest = MessageDigest.getInstance("SHA-256").digest(first.toByteArray())
        return digest.joinToString(":") { "%02X".format(it) }
    }
}
