package io.github.apknamer.data.settings

import android.content.Context
import io.github.apknamer.domain.AppSettings
import io.github.apknamer.domain.ListOptions
import io.github.apknamer.domain.NameTemplate
import io.github.apknamer.domain.RenameOptions
import io.github.apknamer.domain.SortOrder
import io.github.apknamer.domain.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Settings live in SharedPreferences (not DataStore) on purpose: they are read synchronously at start-up,
 * so the theme and language are correct on the very first frame, without a flash.
 */
class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    @Synchronized
    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        write(next)
        _settings.value = next
    }

    private fun read() = AppSettings(
        rename = RenameOptions(
            template = prefs.getString("template", null).let { stored ->
                if (stored == null || stored == NameTemplate.LEGACY_DEFAULT) NameTemplate.DEFAULT else stored
            },
            replaceSpaces = prefs.getBoolean("replace_spaces", false),
            fixExtension = prefs.getBoolean("fix_extension", true),
        ),
        deepScan = prefs.getBoolean("deep_scan", false),
        autoScan = prefs.getBoolean("auto_scan", false),
        recursive = prefs.getBoolean("recursive", true),
        lastRoot = prefs.getString("last_root", null),
        themeMode = enumOf(prefs.getString("theme", null), ThemeMode.SYSTEM),
        dynamicColor = prefs.getBoolean("dynamic_color", true),
        pureBlack = prefs.getBoolean("pure_black", false),
        language = prefs.getString(KEY_LANGUAGE, null) ?: "system",
        list = ListOptions(
            icons = prefs.getBoolean("list_icons", true),
            oldName = prefs.getBoolean("list_old_name", true),
            versionName = prefs.getBoolean("list_version_name", true),
            versionCode = prefs.getBoolean("list_version_code", true),
            packageName = prefs.getBoolean("list_package", false),
            size = prefs.getBoolean("list_size", true),
            date = prefs.getBoolean("list_date", true),
            path = prefs.getBoolean("list_path", true),
            typeTag = prefs.getBoolean("list_type_tag", true),
            installTag = prefs.getBoolean("list_install_tag", true),
            duplicateTag = prefs.getBoolean("list_duplicate_tag", true),
            status = prefs.getBoolean("list_status", true),
            sdkAbi = prefs.getBoolean("list_sdk_abi", true),
        ),
        sortOrder = enumOf(prefs.getString("sort", null), SortOrder.NAME),
        sortDescending = prefs.getBoolean("sort_desc", false),
        confirmRename = prefs.getBoolean("confirm_rename", true),
        tourSeen = prefs.getBoolean("tour_seen", false),
    )

    private fun write(s: AppSettings) {
        prefs.edit()
            .putString("template", s.rename.template)
            .putBoolean("replace_spaces", s.rename.replaceSpaces)
            .putBoolean("fix_extension", s.rename.fixExtension)
            .putBoolean("deep_scan", s.deepScan)
            .putBoolean("auto_scan", s.autoScan)
            .putBoolean("recursive", s.recursive)
            .putString("last_root", s.lastRoot)
            .putString("theme", s.themeMode.name)
            .putBoolean("dynamic_color", s.dynamicColor)
            .putBoolean("pure_black", s.pureBlack)
            .putString(KEY_LANGUAGE, s.language)
            .putBoolean("list_icons", s.list.icons)
            .putBoolean("list_old_name", s.list.oldName)
            .putBoolean("list_version_name", s.list.versionName)
            .putBoolean("list_version_code", s.list.versionCode)
            .putBoolean("list_package", s.list.packageName)
            .putBoolean("list_size", s.list.size)
            .putBoolean("list_date", s.list.date)
            .putBoolean("list_path", s.list.path)
            .putBoolean("list_type_tag", s.list.typeTag)
            .putBoolean("list_install_tag", s.list.installTag)
            .putBoolean("list_duplicate_tag", s.list.duplicateTag)
            .putBoolean("list_status", s.list.status)
            .putBoolean("list_sdk_abi", s.list.sdkAbi)
            .putString("sort", s.sortOrder.name)
            .putBoolean("sort_desc", s.sortDescending)
            .putBoolean("confirm_rename", s.confirmRename)
            .putBoolean("tour_seen", s.tourSeen)
            .apply()
    }

    private inline fun <reified E : Enum<E>> enumOf(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    companion object {
        const val PREFS_NAME = "settings"
        const val KEY_LANGUAGE = "language"
    }
}
