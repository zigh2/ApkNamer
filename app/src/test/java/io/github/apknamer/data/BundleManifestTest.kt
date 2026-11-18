package io.github.apknamer.data

import io.github.apknamer.data.parse.BundleManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BundleManifestTest {
    @Test fun parsesXapkManifest() {
        val json = """{"package_name":"com.example.app","name":"Example","version_code":"42","version_name":"1.2.3"}"""
        val m = BundleManifest.parseXapk(json)!!
        assertEquals("Example", m.label)
        assertEquals("com.example.app", m.packageName)
        assertEquals("1.2.3", m.versionName)
        assertEquals(42L, m.versionCode)
    }

    @Test fun xapkSdkAndSplitConfigs() {
        val json = """{"package_name":"a.b","min_sdk_version":"21","target_sdk_version":"34",
            "split_configs":["config.arm64_v8a","config.xxhdpi"]}"""
        val m = BundleManifest.parseXapk(json)!!
        assertEquals(21, m.minSdk)
        assertEquals(34, m.targetSdk)
        assertEquals(setOf("arm64-v8a"), m.abis)
    }

    @Test fun apkmMinApiAndArches() {
        val json = """{"pkg_name":"a.b","min_api":"24","arches":["arm64-v8a","armeabi-v7a"]}"""
        val m = BundleManifest.parseApkm(json)!!
        assertEquals(24, m.minSdk)
        assertEquals(setOf("arm64-v8a", "armeabi-v7a"), m.abis)
    }

    @Test fun xapkWithNumericVersionCode() {
        val m = BundleManifest.parseXapk("""{"package_name":"a.b","version_code":7}""")!!
        assertEquals(7L, m.versionCode)
    }

    @Test fun webExtensionManifestIsNotAnXapk() {
        assertNull(BundleManifest.parseXapk("""{"manifest_version":3,"name":"Ext","version":"1.0"}"""))
    }

    @Test fun parsesApkmInfo() {
        val json = """{"pkg_name":"com.example.app","app_name":"Example","versionname":"2.0","versioncode":"200"}"""
        val m = BundleManifest.parseApkm(json)!!
        assertEquals("Example", m.label)
        assertEquals("2.0", m.versionName)
        assertEquals(200L, m.versionCode)
    }

    @Test fun invalidJsonIsIgnored() {
        assertNull(BundleManifest.parseXapk("not json"))
        assertNull(BundleManifest.parseApkm("{"))
    }
}
