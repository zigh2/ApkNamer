package io.github.apknamer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.apknamer.R
import io.github.apknamer.domain.AppSettings
import io.github.apknamer.domain.FileNameSanitizer
import io.github.apknamer.domain.NameTemplate

private val PRESETS = listOf(
    "Name v1.0 (123)" to NameTemplate.DEFAULT,
    "Name_1.0" to "{name}_{version}",
    "Name [package] 1.0" to "{name}< [{package}]>< {version}>",
    "package-1.0-123" to "{package}-{version}-{code}",
)

private val SAMPLE = mapOf(
    "name" to "Telegram",
    "package" to "org.telegram.messenger",
    "version" to "10.14.5",
    "code" to "5034",
    "type" to "APK",
    "original" to "download (1)",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OptionsSheet(
    settings: AppSettings,
    onTemplate: (String) -> Unit,
    onReplaceSpaces: (Boolean) -> Unit,
    onFixExtension: (Boolean) -> Unit,
    onDeepScan: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        var text by rememberSaveable { mutableStateOf(settings.rename.template) }
        val unknown = NameTemplate.unknownTokens(text)
        val preview = FileNameSanitizer.sanitize(NameTemplate.render(text, SAMPLE), settings.rename.replaceSpaces) + ".apk"

        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.options), style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onTemplate(it)
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
                            onTemplate(template)
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

            SwitchRow(stringResource(R.string.opt_replace_spaces), settings.rename.replaceSpaces, onReplaceSpaces)
            SwitchRow(stringResource(R.string.opt_fix_extension), settings.rename.fixExtension, onFixExtension)
            SwitchRow(
                stringResource(R.string.opt_deep_scan),
                settings.deepScan,
                onDeepScan,
                description = stringResource(R.string.opt_deep_scan_desc),
            )
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
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChecked)
            .padding(vertical = 4.dp),
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
