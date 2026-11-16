package io.github.apknamer.data.parse

data class ApkDetails(
    val minSdk: Int?,
    val targetSdk: Int?,
    val permissions: List<String>,
    val signerSha256: String?,
)

data class BundleEntry(val name: String, val size: Long)

data class InstalledApp(val versionName: String?, val versionCode: Long)

data class FileDetails(
    val apk: ApkDetails?,
    val contents: List<BundleEntry>,
    val installed: InstalledApp?,
)
