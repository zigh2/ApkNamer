package io.github.apknamer.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.apknamer.domain.AppSettings
import io.github.apknamer.domain.NameTemplate
import io.github.apknamer.domain.RenameOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            rename = RenameOptions(
                template = p[TEMPLATE] ?: NameTemplate.DEFAULT,
                replaceSpaces = p[REPLACE_SPACES] ?: false,
                fixExtension = p[FIX_EXTENSION] ?: true,
            ),
            deepScan = p[DEEP_SCAN] ?: false,
            lastRoot = p[LAST_ROOT],
        )
    }

    suspend fun setTemplate(value: String) = context.dataStore.edit { it[TEMPLATE] = value }
    suspend fun setReplaceSpaces(value: Boolean) = context.dataStore.edit { it[REPLACE_SPACES] = value }
    suspend fun setFixExtension(value: Boolean) = context.dataStore.edit { it[FIX_EXTENSION] = value }
    suspend fun setDeepScan(value: Boolean) = context.dataStore.edit { it[DEEP_SCAN] = value }
    suspend fun setRoot(path: String) = context.dataStore.edit { it[LAST_ROOT] = path }

    private companion object {
        val TEMPLATE = stringPreferencesKey("template")
        val REPLACE_SPACES = booleanPreferencesKey("replace_spaces")
        val FIX_EXTENSION = booleanPreferencesKey("fix_extension")
        val DEEP_SCAN = booleanPreferencesKey("deep_scan")
        val LAST_ROOT = stringPreferencesKey("last_root")
    }
}
