package io.github.apknamer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AbisTest {
    @Test fun readsLibDirectories() {
        val names = sequenceOf("lib/arm64-v8a/libx.so", "lib/x86_64/liby.so", "assets/lib/foo", "classes.dex")
        assertEquals(setOf("arm64-v8a", "x86_64"), Abis.fromLibDirs(names))
    }

    @Test fun readsSplitApkNames() {
        val names = listOf("base.apk", "config.arm64_v8a.apk", "splits/config.armeabi_v7a.apk", "config.xxhdpi.apk")
        assertEquals(setOf("arm64-v8a", "armeabi-v7a"), Abis.fromNames(names))
    }

    @Test fun readsManifestStyleTokens() {
        assertEquals(setOf("arm64-v8a", "x86_64", "x86"), Abis.fromNames(listOf("arm64-v8a", "x86_64", "x86")))
    }

    @Test fun labels() {
        assertEquals("arm64-v8a+armeabi-v7a", Abis.abiLabel(setOf("armeabi-v7a", "arm64-v8a")))
        assertEquals("v8", Abis.archLabel(setOf("arm64-v8a")))
        assertEquals("v7", Abis.archLabel(setOf("armeabi-v7a")))
        assertEquals("arm", Abis.archLabel(setOf("arm64-v8a", "armeabi-v7a")))
        assertEquals("universal", Abis.archLabel(setOf("arm64-v8a", "x86")))
        assertEquals("x64", Abis.archLabel(setOf("x86_64")))
        assertNull(Abis.archLabel(emptySet()))
        assertNull(Abis.abiLabel(emptySet()))
    }
}
