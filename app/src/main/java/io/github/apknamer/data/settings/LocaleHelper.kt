package io.github.apknamer.data.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** Applies the in-app language (works on every supported Android version, no AppCompat needed). */
object LocaleHelper {
    // We ship a universal APK (no per-language app bundle splits), so this warning does not apply.
    @SuppressLint("AppBundleLocaleChanges")
    fun wrap(base: Context): Context {
        val tag = base.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SettingsRepository.KEY_LANGUAGE, null) ?: "system"
        if (tag == "system") {
            Locale.setDefault(Resources.getSystem().configuration.locales[0])
            return base
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
