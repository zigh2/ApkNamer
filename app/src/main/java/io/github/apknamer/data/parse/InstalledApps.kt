package io.github.apknamer.data.parse

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat

/** Reads what is installed on this device (needs QUERY_ALL_PACKAGES on Android 11+). */
class InstalledApps(context: Context) {
    private val pm: PackageManager = context.applicationContext.packageManager

    @Suppress("DEPRECATION")
    fun versionCodes(): Map<String, Long> =
        runCatching {
            pm.getInstalledPackages(0).associate { it.packageName to PackageInfoCompat.getLongVersionCode(it) }
        }.getOrDefault(emptyMap())

    @Suppress("DEPRECATION")
    fun lookup(packageName: String): InstalledApp? =
        runCatching {
            val info = pm.getPackageInfo(packageName, 0)
            InstalledApp(info.versionName, PackageInfoCompat.getLongVersionCode(info))
        }.getOrNull()
}
