package io.github.apknamer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.apknamer.R

@Composable
fun AppRoot(viewModel: MainViewModel, onRequestPermission: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermission() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val args = message.args.toTypedArray()
            val text = if (message.plural != 0) {
                context.resources.getQuantityString(message.plural, message.quantity, *args)
            } else {
                context.getString(message.res, *args)
            }
            val result = snackbarHostState.showSnackbar(
                message = text,
                actionLabel = if (message.undoable) context.getString(R.string.undo) else null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undo()
        }
    }

    BackHandler(enabled = state.screen != Screen.MAIN) { viewModel.back() }

    Crossfade(targetState = state.screen, label = "screen") { screen ->
        when (screen) {
            Screen.MAIN -> MainScreen(state, viewModel, snackbarHostState, onRequestPermission)
            Screen.SETTINGS -> SettingsScreen(state, viewModel, viewModel::back)
            Screen.DETAILS -> DetailsScreen(state, viewModel, snackbarHostState, viewModel::back)
            Screen.DUPLICATES -> DuplicatesScreen(state, viewModel, snackbarHostState, viewModel::back)
        }
    }

    state.diagnostics?.let { report ->
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
                TextButton(onClick = { copyToClipboard(context, report) }) { Text(stringResource(R.string.diag_copy)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDiagnostics) { Text(stringResource(R.string.close)) }
            },
        )
    }
}
