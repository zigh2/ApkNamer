package io.github.apknamer.ui

import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.apknamer.R
import io.github.apknamer.domain.PackageType
import io.github.apknamer.domain.PlanStatus
import io.github.apknamer.domain.Problem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onRequestPermission: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showOptions by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermission() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = context.getString(message.res, *message.args.toTypedArray())
            val result = snackbarHostState.showSnackbar(
                message = text,
                actionLabel = if (message.undoable) context.getString(R.string.undo) else null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undo()
        }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.onFolderPicked(uri)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (state.canUndo && !state.busy) {
                        TextButton(onClick = viewModel::undo) { Text(stringResource(R.string.undo_last)) }
                    }
                    IconButton(onClick = { showOptions = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.options))
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
                    onSelectAll = viewModel::selectAll,
                    onRename = { showConfirm = true },
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
                    onScan = viewModel::startScan,
                    onStop = viewModel::cancelScan,
                    onDownloads = {
                        viewModel.setRoot(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                    },
                    onStorage = { viewModel.setRoot(Environment.getExternalStorageDirectory()) },
                    onChooseFolder = { folderPicker.launch(null) },
                    onToggle = viewModel::toggle,
                )
            }
        }
    }

    if (showOptions) {
        OptionsSheet(
            settings = state.settings,
            onTemplate = viewModel::setTemplate,
            onReplaceSpaces = viewModel::setReplaceSpaces,
            onFixExtension = viewModel::setFixExtension,
            onDeepScan = viewModel::setDeepScan,
            onDismiss = { showOptions = false },
        )
    }

    state.diagnostics?.let { report ->
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = viewModel::dismissDiagnostics,
            title = { Text(stringResource(R.string.diag_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.diag_body), style = MaterialTheme.typography.bodySmall)
                    SelectionContainer {
                        Text(
                            report,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { clipboard.setText(AnnotatedString(report)) }) {
                    Text(stringResource(R.string.diag_copy))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDiagnostics) { Text(stringResource(R.string.close)) }
            },
        )
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.confirm_title)) },
            text = { Text(stringResource(R.string.confirm_body, state.selectedCount)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    viewModel.applyRename()
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
    onScan: () -> Unit,
    onStop: () -> Unit,
    onDownloads: () -> Unit,
    onStorage: () -> Unit,
    onChooseFolder: () -> Unit,
    onToggle: (String) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            LocationCard(state.root, state.scanning, onScan, onStop, onDownloads, onStorage, onChooseFolder)
        }
        if (state.scanning) {
            item { ProgressBlock(state) }
        } else if (!state.hasScanned) {
            item { InfoBlock(R.string.empty_title, R.string.empty_body) }
        } else if (state.items.isEmpty()) {
            item { InfoBlock(R.string.no_results_title, R.string.no_results_body) }
        } else {
            item {
                Text(
                    stringResource(R.string.found_count, state.items.size),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        items(state.items, key = { it.detected.id }) { item ->
            ItemRow(item) { onToggle(item.detected.id) }
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
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.location_label), style = MaterialTheme.typography.labelMedium)
            Text(root.path, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            }
        }
    }
}

@Composable
private fun ProgressBlock(state: UiState) {
    val p = state.progress
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (p.total > 0) {
            LinearProgressIndicator(progress = { p.analyzed / p.total.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.analyze_progress, p.analyzed, p.total), style = MaterialTheme.typography.bodySmall)
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.scan_progress, p.visited), style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun ItemRow(item: ItemUi, onToggle: () -> Unit) {
    val ready = item.plan.status == PlanStatus.READY
    val meta = item.detected.metadata
    val currentName = item.detected.file.name
    val versionText = meta?.versionName?.let { v ->
        if (meta?.versionCode != null) "$v (${meta?.versionCode})" else v
    }
    val details = listOfNotNull(meta?.packageName, versionText).joinToString(" · ")

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = ready, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = item.selected, onCheckedChange = null, enabled = ready)
        Column(Modifier.weight(1f)) {
            Text(
                text = item.plan.target?.name ?: currentName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (ready) {
                Text(
                    text = currentName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
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
            statusText(item)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        if (item.detected.type != PackageType.UNKNOWN) {
            Text(
                item.detected.type.extension.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
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
    Surface(tonalElevation = 3.dp) {
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
