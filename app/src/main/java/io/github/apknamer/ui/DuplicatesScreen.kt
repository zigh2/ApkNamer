@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package io.github.apknamer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.apknamer.R
import io.github.apknamer.data.parse.IconLoader
import io.github.apknamer.domain.DuplicateGroup

/**
 * Review page for duplicates (same app name, same size, same checksum).
 * Select the copies to delete; at least one copy of every app is always kept.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(state: UiState, vm: MainViewModel, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val context = LocalContext.current
    var confirm by rememberSaveable { mutableStateOf(false) }
    val selected = state.dupSelected
    val selectedEntries = state.dupGroups.flatMap { it.entries }.filter { it.detected.id in selected }
    val freed = selectedEntries.sumOf { it.detected.size }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            pluralStringResource(R.plurals.dup_selected, selectedEntries.size, selectedEntries.size),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            stringResource(R.string.dup_frees, formatSize(context, freed)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = { confirm = true },
                        enabled = selectedEntries.isNotEmpty() && !state.busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) { Text(stringResource(R.string.dup_delete_button)) }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.dup_rule), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = vm::autoSelectDuplicates, enabled = !state.busy) {
                            Text(stringResource(R.string.dup_auto))
                        }
                        TextButton(onClick = vm::clearDuplicateSelection, enabled = selected.isNotEmpty()) {
                            Text(stringResource(R.string.dup_clear))
                        }
                    }
                }
            }
            if (state.dupGroups.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.dup_none),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            items(state.dupGroups, key = { it.key }) { group ->
                GroupCard(
                    group = group,
                    selected = selected,
                    loader = vm.iconLoader,
                    enabled = !state.busy,
                    onToggle = vm::toggleDuplicate,
                )
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = {
                Text(pluralStringResource(R.plurals.dup_confirm_title, selectedEntries.size, selectedEntries.size))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.dup_confirm_body, formatSize(context, freed)))
                    Text(
                        selectedEntries.joinToString("\n\n") { it.detected.file.absolutePath },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    vm.deleteSelectedDuplicates()
                }) { Text(stringResource(R.string.delete_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun GroupCard(
    group: DuplicateGroup,
    selected: Set<String>,
    loader: IconLoader,
    enabled: Boolean,
    onToggle: (String) -> Unit,
) {
    val context = LocalContext.current
    val first = group.entries.first().detected
    val kept = group.entries.count { it.detected.id !in selected }
    val anySelected = kept < group.entries.size

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon(first, loader, 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        first.metadata?.label ?: first.metadata?.packageName ?: first.file.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        pluralStringResource(
                            R.plurals.dup_group_info, group.entries.size, group.entries.size, formatSize(context, group.size),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                stringResource(R.string.dup_keeping, kept, group.entries.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            group.entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider()
                val id = entry.detected.id
                val isSelected = id in selected
                // The last remaining copy is locked: it cannot be selected for deletion.
                val locked = !isSelected && kept == 1
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggle(id) }, enabled = enabled && !locked)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            entry.detected.file.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            rememberPrettyPath(entry.detected.file.absolutePath),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            formatDate(context, entry.detected.modified),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (anySelected && !isSelected) {
                            Tag(
                                stringResource(R.string.tag_kept),
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    TextButton(onClick = { openFolder(context, entry.detected.file.parentFile ?: entry.detected.file) }) {
                        Text(stringResource(R.string.action_open_folder))
                    }
                }
            }
        }
    }
}
