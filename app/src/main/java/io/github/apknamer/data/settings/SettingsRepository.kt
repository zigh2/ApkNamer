package io.github.apknamer.data.settings

import android.content.Context
import io.github.apknamer.domain.AppSettings
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
            template = prefs.getString("template", null) ?: NameTemplate.DEFAULT,
            replaceSpaces = prefs.getBoolean("replace_spaces", false),
            fixExtension = prefs.getBoolean("fix_extension", true),
        ),
        deepScan = prefs.getBoolean("deep_scan", false),
        recursive = prefs.getBoolean("recursive", true),
        lastRoot = prefs.getString("last_root", null),
        themeMode = enumOf(prefs.getString("theme", null), ThemeMode.SYSTEM),
        dynamicColor = prefs.getBoolean("dynamic_color", true),
        pureBlack = prefs.getBoolean("pure_black", false),
        language = prefs.getString(KEY_LANGUAGE, null) ?: "system",
        showIcons = prefs.getBoolean("show_icons", true),
        showPaths = prefs.getBoolean("show_paths", true),
        sortOrder = enumOf(prefs.getString("sort", null), SortOrder.NAME),
        sortDescending = prefs.getBoolean("sort_desc", false),
        confirmRename = prefs.getBoolean("confirm_rename", true),
    )

    private fun write(s: AppSettings) {
        prefs.edit()
            .putString("template", s.rename.template)
            .putBoolean("replace_spaces", s.rename.replaceSpaces)
            .putBoolean("fix_extension", s.rename.fixExtension)
            .putBoolean("deep_scan", s.deepScan)
            .putBoolean("recursive", s.recursive)
            .putString("last_root", s.lastRoot)
            .putString("theme", s.themeMode.name)
            .putBoolean("dynamic_color", s.dynamicColor)
            .putBoolean("pure_black", s.pureBlack)
            .putString(KEY_LANGUAGE, s.language)
            .putBoolean("show_icons", s.showIcons)
            .putBoolean("show_paths", s.showPaths)
            .putString("sort", s.sortOrder.name)
            .putBoolean("sort_desc", s.sortDescending)
            .putBoolean("confirm_rename", s.confirmRename)
            .apply()
    }

    private inline fun <reified E : Enum<E>> enumOf(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    companion object {
        const val PREFS_NAME = "settings"
        const val KEY_LANGUAGE = "language"
    }
}
