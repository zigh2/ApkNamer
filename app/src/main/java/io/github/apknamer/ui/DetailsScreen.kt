package io.github.apknamer.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.apknamer.R
import io.github.apknamer.domain.InstallState
import io.github.apknamer.domain.PackageType
import io.github.apknamer.domain.PlanStatus
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailsScreen(state: UiState, vm: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val entry = state.detailsEntry

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (entry == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.details_unavailable))
            }
            return@Scaffold
        }

        val file = entry.detected
        val meta = file.metadata
        val apk = state.details?.apk
        val installed = state.details?.installed
        val packageName = meta?.packageName
        var showPermissions by rememberSaveable { mutableStateOf(false) }

        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AppIcon(file, vm.iconLoader, 72.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        meta?.label ?: packageName ?: file.file.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (packageName != null) Text(packageName, style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (file.type != PackageType.UNKNOWN) {
                            Tag(
                                file.type.extension.uppercase(),
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        InstallTag(entry.install)
                        if (entry.duplicate) {
                            Tag(
                                stringResource(R.string.tag_duplicate),
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
            if (state.detailsLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

            // Proposed name
            if (entry.plan.status == PlanStatus.READY) {
                InfoCard(stringResource(R.string.details_proposed)) {
                    SelectionContainer {
                        Text(
                            entry.plan.target?.name.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        file.file.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough,
                    )
                    Button(onClick = { vm.renameOne(file.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.details_rename))
                    }
                }
            }

            // App
            InfoCard(stringResource(R.string.details_section_app)) {
                InfoRow(stringResource(R.string.label_package), packageName)
                InfoRow(stringResource(R.string.label_version_name), meta?.versionName)
                InfoRow(stringResource(R.string.label_version_code), meta?.versionCode?.toString())
                apk?.minSdk?.takeIf { it > 0 }?.let { InfoRow(stringResource(R.string.label_min_sdk), sdkText(it)) }
                apk?.targetSdk?.takeIf { it > 0 }?.let { InfoRow(stringResource(R.string.label_target_sdk), sdkText(it)) }
                if (apk != null) {
                    InfoRow(stringResource(R.string.label_signature), apk.signerSha256, mono = true)
                    Column {
                        Text(
                            stringResource(R.string.label_permissions),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (apk.permissions.isEmpty()) {
                            Text(stringResource(R.string.permissions_none), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.permissions_count, apk.permissions.size),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { showPermissions = !showPermissions }) {
                                    Text(stringResource(if (showPermissions) R.string.hide else R.string.show_all))
                                }
                            }
                            if (showPermissions) {
                                SelectionContainer {
                                    Text(
                                        apk.permissions.joinToString("\n") { it.removePrefix("android.permission.") },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // This device
            if (packageName != null && !state.detailsLoading) {
                InfoCard(stringResource(R.string.details_section_device)) {
                    if (installed == null) {
                        Text(stringResource(R.string.installed_none), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        InfoRow(
                            stringResource(R.string.installed_label),
                            "${installed.versionName ?: "?"} (${installed.versionCode})",
                        )
                        Text(
                            stringResource(
                                when (entry.install) {
                                    InstallState.UPDATE -> R.string.install_update
                                    InstallState.DOWNGRADE -> R.string.install_older
                                    else -> R.string.install_same
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // File
            InfoCard(stringResource(R.string.details_section_file)) {
                InfoRow(stringResource(R.string.label_name), file.file.name)
                InfoRow(stringResource(R.string.label_folder), rememberPrettyPath(file.file.parent.orEmpty()))
                InfoRow(stringResource(R.string.label_path), file.file.absolutePath, mono = true)
                InfoRow(
                    stringResource(R.string.label_size),
                    stringResource(
                        R.string.size_value,
                        formatSize(context, file.size),
                        NumberFormat.getInstance().format(file.size),
                    ),
                )
                InfoRow(stringResource(R.string.label_modified), formatDate(context, file.modified))
                InfoRow(stringResource(R.string.label_format), file.type.extension.uppercase().ifEmpty { null })
            }

            // Bundle contents
            val contents = state.details?.contents.orEmpty()
            if (contents.isNotEmpty()) {
                InfoCard(stringResource(R.string.details_section_contents)) {
                    contents.forEach { part ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                part.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (part.size >= 0) formatSize(context, part.size) else "",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            // Actions
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { copyToClipboard(context, file.file.absolutePath) }) {
                    Text(stringResource(R.string.action_copy_path))
                }
                if (packageName != null) {
                    OutlinedButton(onClick = { copyToClipboard(context, packageName) }) {
                        Text(stringResource(R.string.action_copy_package))
                    }
                    OutlinedButton(onClick = { openStore(context, packageName) }) {
                        Text(stringResource(R.string.action_store))
                    }
                }
                if (installed != null && packageName != null) {
                    OutlinedButton(onClick = { openAppInfo(context, packageName) }) {
                        Text(stringResource(R.string.action_app_info))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?, mono: Boolean = false) {
    if (value.isNullOrBlank()) return
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            )
        }
    }
}

@Composable
private fun sdkText(api: Int): String {
    val name = androidVersionName(api)
    return if (name != null) stringResource(R.string.sdk_value, name, api) else "API $api"
}

private fun openStore(context: Context, packageName: String) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")),
            )
        }
    }
}

private fun openAppInfo(context: Context, packageName: String) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
        )
    }
}
