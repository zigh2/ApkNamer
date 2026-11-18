package io.github.apknamer.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.apknamer.R
import io.github.apknamer.domain.FileNameSanitizer
import io.github.apknamer.domain.ListOptions
import io.github.apknamer.domain.NameTemplate
import io.github.apknamer.domain.SortOrder
import io.github.apknamer.domain.ThemeMode

private val PRESETS = listOf(
    "Name v1.0 (123)" to NameTemplate.DEFAULT,
    "Name_1.0" to "{name}_{version}",
    "Name [package] 1.0" to "{name}< [{package}]>< {version}>",
    "package-1.0-123" to "{package}-{version}-{code}",
    "Name 1.0 v8 (Android 8.0+)" to "{name}< {version}>< {arch}>< (Android {minandroid}+)>",
)

private val SAMPLE = mapOf(
    "name" to "Telegram",
    "package" to "org.telegram.messenger",
    "version" to "10.14.5",
    "code" to "5034",
    "type" to "APK",
    "original" to "download (1)",
    "minsdk" to "26",
    "minandroid" to "8.0",
    "targetsdk" to "34",
    "targetandroid" to "14",
    "abi" to "arm64-v8a",
    "arch" to "v8",
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(state: UiState, vm: MainViewModel, onBack: () -> Unit) {
    val s = state.settings
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Section(stringResource(R.string.section_appearance)) {
                ChoiceRow(
                    stringResource(R.string.theme),
                    listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.DARK to stringResource(R.string.theme_dark),
                    ),
                    s.themeMode,
                ) { mode -> vm.updateSettings { it.copy(themeMode = mode) } }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow(
                        stringResource(R.string.dynamic_color),
                        s.dynamicColor,
                        { v -> vm.updateSettings { it.copy(dynamicColor = v) } },
                        stringResource(R.string.dynamic_color_desc),
                    )
                }
                SwitchRow(
                    stringResource(R.string.pure_black),
                    s.pureBlack,
                    { v -> vm.updateSettings { it.copy(pureBlack = v) } },
                    stringResource(R.string.pure_black_desc),
                )
                ChoiceRow(
                    stringResource(R.string.language),
                    listOf(
                        "system" to stringResource(R.string.lang_system),
                        "en" to "English",
                        "ar" to "العربية",
                    ),
                    s.language,
                ) { tag ->
                    if (tag != s.language) {
                        vm.updateSettings { it.copy(language = tag) }
                        context.findActivity()?.recreate()
                    }
                }
            }

            Section(stringResource(R.string.section_scanning)) {
                SwitchRow(
                    stringResource(R.string.auto_scan),
                    s.autoScan,
                    { v -> vm.updateSettings { it.copy(autoScan = v) } },
                )
                SwitchRow(
                    stringResource(R.string.subfolders),
                    s.recursive,
                    { v -> vm.updateSettings { it.copy(recursive = v) } },
                )
                SwitchRow(
                    stringResource(R.string.opt_deep_scan),
                    s.deepScan,
                    { v -> vm.updateSettings { it.copy(deepScan = v) } },
                    stringResource(R.string.opt_deep_scan_desc),
                )
            }

            Section(stringResource(R.string.section_naming)) {
                var text by rememberSaveable { mutableStateOf(s.rename.template) }
                LaunchedEffect(s.rename.template) { if (text != s.rename.template) text = s.rename.template }
                val unknown = NameTemplate.unknownTokens(text)
                val preview = FileNameSanitizer.sanitize(NameTemplate.render(text, SAMPLE), s.rename.replaceSpaces) + ".apk"

                OutlinedTextField(
                    value = text,
                    onValueChange = { v ->
                        text = v
                        vm.updateSettings { it.copy(rename = it.rename.copy(template = v)) }
                    },
                    label = { Text(stringResource(R.string.template_label)) },
                    singleLine = true,
                    isError = unknown.isNotEmpty(),
                    supportingText = {
                        if (unknown.isNotEmpty()) {
                            Text(stringResource(R.string.template_unknown, unknown.joinToString(", ")))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.template_help), style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESETS.forEach { (label, template) ->
                        AssistChip(
                            onClick = {
                                text = template
                                vm.updateSettings { it.copy(rename = it.rename.copy(template = template)) }
                            },
                            label = { Text(label) },
                        )
                    }
                }
                Text(
                    stringResource(R.string.preview, preview),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                SwitchRow(
                    stringResource(R.string.opt_replace_spaces),
                    s.rename.replaceSpaces,
                    { v -> vm.updateSettings { it.copy(rename = it.rename.copy(replaceSpaces = v)) } },
                )
                SwitchRow(
                    stringResource(R.string.opt_fix_extension),
                    s.rename.fixExtension,
                    { v -> vm.updateSettings { it.copy(rename = it.rename.copy(fixExtension = v)) } },
                )
                SwitchRow(
                    stringResource(R.string.confirm_rename),
                    s.confirmRename,
                    { v -> vm.updateSettings { it.copy(confirmRename = v) } },
                )
            }

            Section(stringResource(R.string.section_row)) {
                val l = s.list
                val setList: ((ListOptions) -> ListOptions) -> Unit =
                    { change -> vm.updateSettings { it.copy(list = change(it.list)) } }
                ChoiceRow(
                    stringResource(R.string.list_preset),
                    listOf(
                        ListOptions.COMPACT to stringResource(R.string.preset_compact),
                        ListOptions.STANDARD to stringResource(R.string.preset_standard),
                        ListOptions.DETAILED to stringResource(R.string.preset_detailed),
                    ),
                    l,
                ) { preset -> setList { preset } }
                SwitchRow(stringResource(R.string.show_icons), l.icons, { v -> setList { it.copy(icons = v) } })
                SwitchRow(stringResource(R.string.field_old_name), l.oldName, { v -> setList { it.copy(oldName = v) } })
                SwitchRow(stringResource(R.string.field_version_name), l.versionName, { v -> setList { it.copy(versionName = v) } })
                SwitchRow(stringResource(R.string.field_version_code), l.versionCode, { v -> setList { it.copy(versionCode = v) } })
                SwitchRow(stringResource(R.string.field_package), l.packageName, { v -> setList { it.copy(packageName = v) } })
                SwitchRow(stringResource(R.string.field_sdk_abi), l.sdkAbi, { v -> setList { it.copy(sdkAbi = v) } })
                SwitchRow(stringResource(R.string.field_size), l.size, { v -> setList { it.copy(size = v) } })
                SwitchRow(stringResource(R.string.field_date), l.date, { v -> setList { it.copy(date = v) } })
                SwitchRow(stringResource(R.string.show_paths), l.path, { v -> setList { it.copy(path = v) } })
                SwitchRow(stringResource(R.string.field_type_tag), l.typeTag, { v -> setList { it.copy(typeTag = v) } })
                SwitchRow(stringResource(R.string.field_install_tag), l.installTag, { v -> setList { it.copy(installTag = v) } })
                SwitchRow(stringResource(R.string.field_duplicate_tag), l.duplicateTag, { v -> setList { it.copy(duplicateTag = v) } })
                SwitchRow(stringResource(R.string.field_status), l.status, { v -> setList { it.copy(status = v) } })
            }

            Section(stringResource(R.string.section_list)) {
                ChoiceRow(
                    stringResource(R.string.sort_by),
                    SortOrder.entries.map { it to sortName(it) },
                    s.sortOrder,
                ) { order -> vm.updateSettings { it.copy(sortOrder = order) } }
                SwitchRow(
                    stringResource(R.string.sort_descending),
                    s.sortDescending,
                    { v -> vm.updateSettings { it.copy(sortDescending = v) } },
                )
            }

            Section(stringResource(R.string.section_about)) {
                Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.about_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = vm::resetSettings) { Text(stringResource(R.string.reset_defaults)) }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, start = 4.dp),
    )
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceRow(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChecked),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
