package io.github.apknamer.data.parse

import io.github.apknamer.domain.Abis
import io.github.apknamer.domain.AppMetadata
import org.json.JSONException
import org.json.JSONObject

/** Parsers for the small JSON descriptors that bundle formats ship next to their APKs. */
object BundleManifest {

    /** `manifest.json` of an XAPK (APKPure and compatible). Requires `package_name`. */
    fun parseXapk(json: String): AppMetadata? {
        val o = parse(json) ?: return null
        val pkg = o.str("package_name") ?: return null
        return AppMetadata(
            label = o.str("name"),
            packageName = pkg,
            versionName = o.str("version_name"),
            versionCode = o.str("version_code")?.toLongOrNull(),
            minSdk = o.str("min_sdk_version")?.toIntOrNull(),
            targetSdk = o.str("target_sdk_version")?.toIntOrNull(),
            abis = Abis.fromNames(o.strings("split_configs")),
        )
    }

    /** `info.json` of an APKM (APKMirror). Requires `pkg_name`. */
    fun parseApkm(json: String): AppMetadata? {
        val o = parse(json) ?: return null
        val pkg = o.str("pkg_name") ?: return null
        return AppMetadata(
            label = o.str("app_name"),
            packageName = pkg,
            versionName = o.str("versionname") ?: o.str("release_version"),
            versionCode = o.str("versioncode")?.toLongOrNull(),
            minSdk = o.str("min_api")?.toIntOrNull(),
            abis = Abis.fromNames(o.strings("arches")),
        )
    }

    private fun parse(json: String): JSONObject? =
        try {
            JSONObject(json.removePrefix("\uFEFF"))
        } catch (_: JSONException) {
            null
        }

    private fun JSONObject.strings(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return List(array.length()) { array.optString(it) }
    }

    private fun JSONObject.str(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).trim().ifEmpty { null } else null
}
