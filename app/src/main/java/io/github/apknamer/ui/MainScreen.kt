@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package io.github.apknamer.ui

import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.apknamer.R
import io.github.apknamer.data.parse.IconLoader
import io.github.apknamer.domain.Abis
import io.github.apknamer.domain.AppSettings
import io.github.apknamer.domain.CleanupPlan
import io.github.apknamer.domain.Filter
import io.github.apknamer.domain.InstallState
import io.github.apknamer.domain.PackageType
import io.github.apknamer.domain.PlanStatus
import io.github.apknamer.domain.Problem
import io.github.apknamer.domain.SortOrder
import java.io.File

private val FILTERS = listOf(
    Filter.ALL, Filter.NEEDS_RENAME, Filter.NAMED, Filter.APK, Filter.XAPK, Filter.APKM, Filter.APKS, Filter.UPDATES, Filter.DUPLICATES,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: UiState,
    vm: MainViewModel,
    snackbar: SnackbarHostState,
    onRequestPermission: () -> Unit,
) {
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.onFolderPicked(uri)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (state.canUndo && !state.busy) {
                        TextButton(onClick = vm::undo) { Text(stringResource(R.string.undo_last)) }
                    }
                    IconButton(
                        onClick = vm::openSettings,
                        modifier = Modifier.tourTarget(LocalTourBounds.current, "settings"),
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
        bottomBar = {
            if (state.hasPermission && state.items.isNotEmpty()) {
                SelectionBar(
                    selected = state.selectedCount,
                    ready = state.readyCount,
                    busy = state.busy,
                    onSelectAll = vm::selectAll,
                    onRename = { if (state.settings.confirmRename) showConfirm = true else vm.applyRename() },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (!state.hasPermission) {
                PermissionContent(onRequestPermission)
            } else {
                ResultsList(
                    state = state,
                    loader = vm.iconLoader,
                    onScan = vm::startScan,
                    onStop = vm::cancelScan,
                    onDownloads = {
                        vm.setRoot(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                    },
                    onStorage = { vm.setRoot(Environment.getExternalStorageDirectory()) },
                    onChooseFolder = { folderPicker.launch(null) },
                    onToggle = vm::toggle,
                    onOpen = vm::openDetails,
                    onQuery = vm::setQuery,
                    onFilter = vm::setFilter,
                    onSettings = { transform -> vm.updateSettings(transform) },
                    onCleanup = vm::openDuplicates,
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.confirm_title)) },
            text = { Text(pluralStringResource(R.plurals.confirm_body, state.selectedCount, state.selectedCount)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    vm.applyRename()
                }) { Text(stringResource(R.string.confirm_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun PermissionContent(onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.permission_body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onGrant) { Text(stringResource(R.string.permission_grant)) }
    }
}

@Composable
private fun ResultsList(
    state: UiState,
    loader: IconLoader,
    onScan: () -> Unit,
    onStop: () -> Unit,
    onDownloads: () -> Unit,
    onStorage: () -> Unit,
    onChooseFolder: () -> Unit,
    onToggle: (String) -> Unit,
    onOpen: (String) -> Unit,
    onQuery: (String) -> Unit,
    onFilter: (Filter) -> Unit,
    onSettings: ((AppSettings) -> AppSettings) -> Unit,
    onCleanup: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            LocationCard(state.root, state.scanning, onScan, onStop, onDownloads, onStorage, onChooseFolder)
        }
        if (state.scanning) {
            item { ProgressBlock(state) }
        } else if (!state.hasScanned) {
            item { InfoBlock(R.string.empty_title, R.string.empty_body) }
        } else if (state.totalFound == 0) {
            item { InfoBlock(R.string.no_results_title, R.string.no_results_body) }
        } else {
            item { SearchField(state.query, onQuery) }
            item { FilterRow(state, onFilter, onSettings) }
            item {
                Text(
                    if (state.items.size == state.totalFound) {
                        pluralStringResource(R.plurals.found_count, state.totalFound, state.totalFound)
                    } else {
                        pluralStringResource(R.plurals.found_count_filtered, state.totalFound, state.items.size, state.totalFound)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                Text(
                    stringResource(R.string.stats_total, formatSize(context, state.totalBytes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (state.cleanup.delete.isNotEmpty()) item { CleanupCard(state.cleanup, onCleanup) }
            if (state.items.isEmpty()) {
                item {
                    val allNamed = state.filter == Filter.NEEDS_RENAME && state.query.isEmpty() &&
                        state.renameCount == 0 && state.namedCount > 0
                    if (allNamed) {
                        InfoBlock(R.string.all_named_title, R.string.all_named_body)
                    } else {
                        InfoBlock(R.string.no_results_title, R.string.no_matches)
                    }
                }
            }
        }
        items(state.items, key = { it.detected.id }) { item ->
            ItemRow(item, loader, state.settings, onToggle = { onToggle(item.detected.id) }, onOpen = { onOpen(item.detected.id) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun LocationCard(
    root: File,
    scanning: Boolean,
    onScan: () -> Unit,
    onStop: () -> Unit,
    onDownloads: () -> Unit,
    onStorage: () -> Unit,
    onChooseFolder: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth().padding(16.dp).tourTarget(LocalTourBounds.current, "location")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.location_label), style = MaterialTheme.typography.labelMedium)
            Text(
                rememberPrettyPath(root.path),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    OutlinedButton(onClick = { menuOpen = true }, enabled = !scanning) {
                        Text(stringResource(R.string.location_change))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.root_downloads)) },
                            onClick = { menuOpen = false; onDownloads() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.root_storage)) },
                            onClick = { menuOpen = false; onStorage() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.root_choose)) },
                            onClick = { menuOpen = false; onChooseFolder() },
                        )
                    }
                }
                Button(onClick = if (scanning) onStop else onScan) {
                    Text(stringResource(if (scanning) R.string.scan_stop else R.string.scan))
                }
                TextButton(onClick = { openFolder(context, root) }) {
                    Text(stringResource(R.string.action_open_folder))
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Clear, contentDescription = null) }
            }
        },
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}

@Composable
private fun FilterRow(
    state: UiState,
    onFilter: (Filter) -> Unit,
    onSettings: ((AppSettings) -> AppSettings) -> Unit,
) {
    var sortOpen by remember { mutableStateOf(false) }
    val settings = state.settings
    LazyRow(
        modifier = Modifier.tourTarget(LocalTourBounds.current, "filters"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Box {
                AssistChip(
                    onClick = { sortOpen = true },
                    label = { Text(stringResource(R.string.sort_label, sortName(settings.sortOrder))) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                )
                DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(sortName(order)) },
                            leadingIcon = {
                                if (order == settings.sortOrder) Icon(Icons.Default.Check, contentDescription = null)
                            },
                            onClick = {
                                sortOpen = false
                                onSettings { it.copy(sortOrder = order) }
                            },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_descending)) },
                        trailingIcon = { Checkbox(checked = settings.sortDescending, onCheckedChange = null) },
                        onClick = { onSettings { it.copy(sortDescending = !it.sortDescending) } },
                    )
                }
            }
        }
        items(FILTERS) { filter ->
            val red = MaterialTheme.colorScheme.error
            val green = successColor()
            val colors = when (filter) {
                Filter.NEEDS_RENAME -> FilterChipDefaults.filterChipColors(
                    containerColor = red.copy(alpha = 0.12f),
                    selectedContainerColor = red.copy(alpha = 0.30f),
                )
                Filter.NAMED -> FilterChipDefaults.filterChipColors(
                    containerColor = green.copy(alpha = 0.12f),
                    selectedContainerColor = green.copy(alpha = 0.30f),
                )
                else -> FilterChipDefaults.filterChipColors()
            }
            val count = when (filter) {
                Filter.NEEDS_RENAME -> state.renameCount
                Filter.NAMED -> state.namedCount
                else -> null
            }
            FilterChip(
                selected = state.filter == filter,
                onClick = { onFilter(filter) },
                label = { Text(if (count != null) filterLabel(filter) + " " + count else filterLabel(filter)) },
                colors = colors,
            )
        }
    }
}

@Composable
fun sortName(order: SortOrder): String = stringResource(
    when (order) {
        SortOrder.NAME -> R.string.sort_name
        SortOrder.APP_NAME -> R.string.sort_app
        SortOrder.SIZE -> R.string.sort_size
        SortOrder.DATE -> R.string.sort_date
        SortOrder.VERSION -> R.string.sort_version
    },
)

@Composable
private fun filterLabel(filter: Filter): String = when (filter) {
    Filter.ALL -> stringResource(R.string.filter_all)
    Filter.NEEDS_RENAME -> stringResource(R.string.filter_rename)
    Filter.NAMED -> stringResource(R.string.filter_named)
    Filter.APK -> "APK"
    Filter.XAPK -> "XAPK"
    Filter.APKM -> "APKM"
    Filter.APKS -> "APKS"
    Filter.UPDATES -> stringResource(R.string.filter_updates)
    Filter.DUPLICATES -> stringResource(R.string.filter_duplicates)
}

@Composable
private fun ProgressBlock(state: UiState) {
    val p = state.progress
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (p.hashTotal > 0) {
            LinearProgressIndicator(progress = { p.hashed / p.hashTotal.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.hash_progress, p.hashed, p.hashTotal), style = MaterialTheme.typography.bodySmall)
        } else if (p.total > 0) {
            LinearProgressIndicator(progress = { p.analyzed / p.total.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.analyze_progress, p.analyzed, p.total), style = MaterialTheme.typography.bodySmall)
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(pluralStringResource(R.plurals.scan_progress, p.visited, p.visited), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InfoBlock(title: Int, body: Int) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemRow(
    item: ItemUi,
    loader: IconLoader,
    settings: AppSettings,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val context = LocalContext.current
    val lo = settings.list
    val ready = item.plan.status == PlanStatus.READY
    val meta = item.detected.metadata
    val currentName = item.detected.file.name
    val code = meta?.versionCode
    val versionText = when {
        lo.versionName && lo.versionCode ->
            meta?.versionName?.let { v -> if (code != null) "$v ($code)" else v } ?: code?.toString()
        lo.versionName -> meta?.versionName
        lo.versionCode -> code?.toString()
        else -> null
    }
    val minAndroid = if (lo.sdkAbi) {
        meta?.minSdk?.let { api ->
            androidVersionName(api)?.let { stringResource(R.string.sdk_min_short, it) } ?: "API $api+"
        }
    } else {
        null
    }
    val arch = if (lo.sdkAbi) Abis.archLabel(meta?.abis.orEmpty()) else null
    val details = listOfNotNull(
        versionText,
        minAndroid,
        arch,
        if (lo.size) formatSize(context, item.detected.size) else null,
        if (lo.date) formatDate(context, item.detected.modified) else null,
    ).joinToString(" \u00B7 ")
    val folder = rememberPrettyPath(item.detected.file.parent.orEmpty())
    val install = item.entry.install
    val showType = lo.typeTag && item.detected.type != PackageType.UNKNOWN
    val showInstall = lo.installTag &&
        (install == InstallState.UPDATE || install == InstallState.SAME || install == InstallState.DOWNGRADE)
    val showDuplicate = lo.duplicateTag && item.entry.duplicate
    val success = successColor()
    // Already correctly named: a very light green wash that works in light and dark themes.
    val rowTint = if (item.plan.status == PlanStatus.UNCHANGED) success.copy(alpha = 0.10f) else Color.Transparent

    Row(
        Modifier
            .fillMaxWidth()
            .background(rowTint)
            .clickable(onClick = onOpen)
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.selected, onCheckedChange = { onToggle() }, enabled = ready)
        if (lo.icons) {
            AppIcon(item.detected, loader, 44.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.plan.target?.name ?: currentName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (lo.oldName && ready) {
                Text(
                    text = currentName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val packageLine = if (lo.packageName) meta?.packageName else null
            if (packageLine != null) {
                Text(
                    text = packageLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (lo.path) {
                Text(
                    text = folder,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (lo.status) {
                statusText(item)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.plan.status == PlanStatus.UNCHANGED) success else MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (showType || showInstall || showDuplicate) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (showType) {
                        Tag(
                            item.detected.type.extension.uppercase(),
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    if (showInstall) InstallTag(install)
                    if (showDuplicate) {
                        Tag(
                            stringResource(R.string.tag_duplicate),
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun statusText(item: ItemUi): String? = when {
    item.plan.status == PlanStatus.UNCHANGED -> stringResource(R.string.status_unchanged)
    item.plan.status == PlanStatus.UNREADABLE -> when (item.detected.problem) {
        Problem.NOT_AN_ARCHIVE -> stringResource(R.string.problem_not_archive)
        Problem.CORRUPT -> stringResource(R.string.problem_corrupt)
        Problem.NOT_AN_APP -> stringResource(R.string.problem_not_app)
        Problem.NO_METADATA, null -> stringResource(R.string.problem_no_metadata)
    }
    else -> null
}

@Composable
private fun SelectionBar(
    selected: Int,
    ready: Int,
    busy: Boolean,
    onSelectAll: (Boolean) -> Unit,
    onRename: () -> Unit,
) {
    Surface(modifier = Modifier.tourTarget(LocalTourBounds.current, "selection"), tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.selected_of, selected, ready), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { onSelectAll(selected < ready) }, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(if (selected < ready) R.string.select_all else R.string.select_none))
                }
            }
            Button(onClick = onRename, enabled = selected > 0 && !busy) {
                Text(stringResource(R.string.rename_n, selected))
            }
        }
    }
}

@Composable
private fun CleanupCard(plan: CleanupPlan, onCleanup: () -> Unit) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                pluralStringResource(R.plurals.cleanup_summary, plan.delete.size, plan.delete.size, formatSize(context, plan.bytes)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onCleanup) { Text(stringResource(R.string.cleanup_button)) }
        }
    }
}
