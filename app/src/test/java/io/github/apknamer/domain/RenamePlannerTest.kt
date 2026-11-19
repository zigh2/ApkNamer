package io.github.apknamer.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RenamePlannerTest {
    private val meta = AppMetadata("Telegram", "org.telegram.messenger", "10.14.5", 5034)
    private val options = RenameOptions(template = NameTemplate.LEGACY_DEFAULT)
    private val none: (File) -> Boolean = { false }

    private fun item(path: String, type: PackageType = PackageType.APK, m: AppMetadata? = meta) =
        DetectedFile(File(path), type, m)

    @Test fun plansRename() {
        val plan = RenamePlanner.plan(listOf(item("/dl/download.apk")), options, none).single()
        assertEquals(PlanStatus.READY, plan.status)
        assertEquals("Telegram v10.14.5 (5034).apk", plan.target?.name)
    }

    @Test fun unreadableWithoutMetadata() {
        val plan = RenamePlanner.plan(listOf(item("/dl/x.apk", m = null)), options, none).single()
        assertEquals(PlanStatus.UNREADABLE, plan.status)
        assertNull(plan.target)
    }

    @Test fun duplicatesGetNumericSuffix() {
        val plans = RenamePlanner.plan(listOf(item("/dl/a.apk"), item("/dl/b.apk")), options, none)
        assertEquals("Telegram v10.14.5 (5034).apk", plans[0].target?.name)
        assertEquals("Telegram v10.14.5 (5034) (2).apk", plans[1].target?.name)
    }

    @Test fun existingFilesAreNeverOverwritten() {
        val taken = File("/dl/Telegram v10.14.5 (5034).apk").absolutePath
        val plan = RenamePlanner.plan(listOf(item("/dl/a.apk")), options) { it.absolutePath == taken }.single()
        assertEquals("Telegram v10.14.5 (5034) (2).apk", plan.target?.name)
    }

    @Test fun alreadyNamedIsUnchanged() {
        val plan = RenamePlanner.plan(listOf(item("/dl/Telegram v10.14.5 (5034).apk")), options, none).single()
        assertEquals(PlanStatus.UNCHANGED, plan.status)
    }

    @Test fun extensionIsFixedFromDetectedType() {
        val plan = RenamePlanner.plan(listOf(item("/dl/app.zip", PackageType.XAPK)), options, none).single()
        assertEquals("xapk", plan.target?.extension)
    }

    @Test fun extensionAliasIsPreserved() {
        val plan = RenamePlanner.plan(listOf(item("/dl/app.apkx", PackageType.APKS)), options, none).single()
        assertEquals("apkx", plan.target?.extension)
    }

    @Test fun extensionFixCanBeDisabled() {
        val opts = options.copy(fixExtension = false)
        val plan = RenamePlanner.plan(listOf(item("/dl/app.zip", PackageType.XAPK)), opts, none).single()
        assertEquals("zip", plan.target?.extension)
    }

    @Test fun defaultTemplateForNewInstalls() {
        val m = meta.copy(minSdk = 26, targetSdk = 34, abis = setOf("arm64-v8a"))
        val plan = RenamePlanner.plan(listOf(item("/dl/a.apk", m = m)), RenameOptions(), none).single()
        assertEquals("Telegram V10.14.5 (Android 8.0 to 14) v8a.apk", plan.target?.name)
    }

    @Test fun androidAndCpuTokens() {
        val m = meta.copy(minSdk = 26, targetSdk = 34, abis = setOf("arm64-v8a"))
        val opts = options.copy(template = "{name} {arch} Android {minandroid}+ (API {minsdk}, target {targetsdk})")
        val plan = RenamePlanner.plan(listOf(item("/dl/a.apk", m = m)), opts, none).single()
        assertEquals("Telegram v8 Android 8.0+ (API 26, target 34).apk", plan.target?.name)
    }

    @Test fun optionalGroupsDropMissingAndroidAndCpuInfo() {
        val opts = options.copy(template = "{name}< {arch}>< Android {minandroid}+>")
        val plan = RenamePlanner.plan(listOf(item("/dl/a.apk")), opts, none).single()
        assertEquals("Telegram.apk", plan.target?.name)
    }

    @Test fun fallsBackToPackageNameWhenLabelMissing() {
        val m = meta.copy(label = null)
        val plan = RenamePlanner.plan(listOf(item("/dl/a.apk", m = m)), options, none).single()
        assertEquals("org.telegram.messenger v10.14.5 (5034).apk", plan.target?.name)
    }
}
