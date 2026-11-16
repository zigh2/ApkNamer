package io.github.apknamer.ui

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.apknamer.R
import io.github.apknamer.data.parse.ApkMetadataReader
import io.github.apknamer.data.parse.ArchiveInspector
import io.github.apknamer.data.rename.RenameHistory
import io.github.apknamer.data.rename.RenameReport
import io.github.apknamer.data.rename.Renamer
import io.github.apknamer.data.scan.FileScanner
import io.github.apknamer.diagnostics.CrashReporter
import io.github.apknamer.diagnostics.ExitReasonReporter
import io.github.apknamer.data.settings.SettingsRepository
import io.github.apknamer.data.storage.StoragePermission
import io.github.apknamer.data.storage.TreeUriPath
import io.github.apknamer.domain.AppSettings
import io.github.apknamer.domain.DetectedFile
import io.github.apknamer.domain.PlanStatus
import io.github.apknamer.domain.RenamePlan
import io.github.apknamer.domain.RenamePlanner
import io.github.apknamer.domain.ScanProgress
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemUi(val detected: DetectedFile, val plan: RenamePlan, val selected: Boolean)

data class UiState(
    val hasPermission: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val root: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
    val scanning: Boolean = false,
    val progress: ScanProgress = ScanProgress(),
    val hasScanned: Boolean = false,
    val items: List<ItemUi> = emptyList(),
    val busy: Boolean = false,
    val canUndo: Boolean = false,
    val diagnostics: String? = null,
) {
    val readyCount: Int get() = items.count { it.plan.status == PlanStatus.READY }
    val selectedCount: Int get() = items.count { it.selected }
}

data class UiMessage(
    @StringRes val res: Int,
    val args: List<Any> = emptyList(),
    val undoable: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val renamer = Renamer(RenameHistory(File(application.filesDir, "rename_history.json")))
    private val scanner = FileScanner(
        ArchiveInspector(ApkMetadataReader(application), File(application.cacheDir, "inspect")),
    )

    private var detected: List<DetectedFile> = emptyList()
    private var selection: Set<String> = emptySet()
    private var scanJob: Job? = null
    private var planJob: Job? = null
    private val lastProgressAt = AtomicLong(0L)

    private val _state = MutableStateFlow(UiState(hasPermission = StoragePermission.isGranted(application)))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { st ->
                    st.copy(settings = settings, root = settings.lastRoot?.let { File(it) } ?: st.root)
                }
                replan()
            }
        }
        viewModelScope.launch {
            val canUndo = renamer.canUndo()
            _state.update { it.copy(canUndo = canUndo) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val parts = listOfNotNull(CrashReporter.consume(application), ExitReasonReporter.lastReport(application))
            if (parts.isNotEmpty()) {
                _state.update { it.copy(diagnostics = parts.joinToString("\n\n----\n\n")) }
            }
        }
    }

    fun dismissDiagnostics() {
        _state.update { it.copy(diagnostics = null) }
    }

    fun refreshPermission() {
        val granted = StoragePermission.isGranted(getApplication())
        _state.update { it.copy(hasPermission = granted) }
    }

    // ---- scanning -------------------------------------------------------------------------

    fun startScan() {
        if (scanJob?.isActive == true) return
        val snapshot = _state.value
        scanJob = viewModelScope.launch {
            _state.update { it.copy(scanning = true, progress = ScanProgress()) }
            try {
                val result = scanner.scan(snapshot.root, snapshot.settings.deepScan) { progress ->
                    // Workers report very often; update the UI at most ~10 times per second.
                    val now = SystemClock.elapsedRealtime()
                    val finished = progress.total > 0 && progress.analyzed == progress.total
                    if (finished || now - lastProgressAt.get() >= 100) {
                        lastProgressAt.set(now)
                        _state.update { it.copy(progress = progress) }
                    }
                }
                detected = result
                selection = result.map { it.id }.toSet()
                _state.update { it.copy(hasScanned = true) }
                replan()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _messages.trySend(UiMessage(R.string.msg_scan_failed))
            } finally {
                _state.update { it.copy(scanning = false) }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
    }

    fun setRoot(dir: File) {
        scanJob?.cancel()
        detected = emptyList()
        selection = emptySet()
        _state.update { it.copy(items = emptyList(), hasScanned = false, root = dir) }
        viewModelScope.launch { settingsRepository.setRoot(dir.absolutePath) }
    }

    fun onFolderPicked(uri: Uri) {
        val dir = TreeUriPath.toFile(uri)?.takeIf { it.isDirectory }
        if (dir == null) {
            _messages.trySend(UiMessage(R.string.msg_folder_unsupported))
        } else {
            setRoot(dir)
        }
    }

    // ---- selection ------------------------------------------------------------------------

    fun toggle(id: String) {
        selection = if (id in selection) selection - id else selection + id
        refreshSelection()
    }

    fun selectAll(all: Boolean) {
        selection = if (all) detected.map { it.id }.toSet() else emptySet()
        refreshSelection()
    }

    private fun refreshSelection() {
        _state.update { st ->
            st.copy(
                items = st.items.map {
                    it.copy(selected = it.plan.status == PlanStatus.READY && it.detected.id in selection)
                },
            )
        }
    }

    // ---- renaming -------------------------------------------------------------------------

    fun applyRename() {
        val plans = _state.value.items.filter { it.selected }.map { it.plan }
        if (plans.isEmpty() || _state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val report = renamer.apply(plans)
            applyReport(report, selectResult = false)
            val moved = report.records.size
            _state.update { it.copy(busy = false, canUndo = it.canUndo || moved > 0) }
            _messages.trySend(
                if (report.failed == 0) {
                    UiMessage(R.string.msg_renamed, listOf(moved), undoable = moved > 0)
                } else {
                    UiMessage(R.string.msg_renamed_failed, listOf(moved, report.failed), undoable = moved > 0)
                },
            )
        }
    }

    fun undo() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val report = renamer.undoLast()
            if (report != null) {
                applyReport(report, selectResult = true)
                _messages.trySend(UiMessage(R.string.msg_undone, listOf(report.records.size)))
            }
            val canUndo = renamer.canUndo()
            _state.update { it.copy(busy = false, canUndo = canUndo) }
        }
    }

    private fun applyReport(report: RenameReport, selectResult: Boolean) {
        val moved = report.records.associate { it.from to it.to }
        detected = detected.map { d -> moved[d.id]?.let { d.copy(file = File(it)) } ?: d }
        selection = selection - moved.keys + (if (selectResult) moved.values.toSet() else emptySet())
        replan()
    }

    // ---- settings -------------------------------------------------------------------------

    fun setTemplate(value: String) = launchSetting { settingsRepository.setTemplate(value) }
    fun setReplaceSpaces(value: Boolean) = launchSetting { settingsRepository.setReplaceSpaces(value) }
    fun setFixExtension(value: Boolean) = launchSetting { settingsRepository.setFixExtension(value) }
    fun setDeepScan(value: Boolean) = launchSetting { settingsRepository.setDeepScan(value) }

    private fun launchSetting(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    // ---- planning -------------------------------------------------------------------------

    private fun replan() {
        planJob?.cancel()
        planJob = viewModelScope.launch(Dispatchers.Default) {
            val snapshot = detected
            val plans = try {
                RenamePlanner.plan(snapshot, _state.value.settings.rename)
            } catch (_: Exception) {
                return@launch
            }
            _state.update { st ->
                st.copy(
                    items = snapshot.zip(plans) { d, p ->
                        ItemUi(d, p, p.status == PlanStatus.READY && d.id in selection)
                    },
                )
            }
        }
    }
}
