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
import io.github.apknamer.data.parse.DetailsLoader
import io.github.apknamer.data.parse.FileDetails
import io.github.apknamer.data.parse.IconLoader
import io.github.apknamer.data.parse.InstalledApps
import io.github.apknamer.data.rename.RenameHistory
import io.github.apknamer.data.rename.RenameReport
import io.github.apknamer.data.rename.Renamer
import io.github.apknamer.data.scan.FileScanner
import io.github.apknamer.data.settings.SettingsRepository
import io.github.apknamer.data.storage.StoragePermission
import io.github.apknamer.data.storage.TreeUriPath
import io.github.apknamer.diagnostics.CrashReporter
import io.github.apknamer.diagnostics.ExitReasonReporter
import io.github.apknamer.domain.AppSettings
import io.github.apknamer.domain.DetectedFile
import io.github.apknamer.domain.Entry
import io.github.apknamer.domain.EntryQuery
import io.github.apknamer.domain.Filter
import io.github.apknamer.domain.InstallStates
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
import kotlinx.coroutines.withContext

enum class Screen { MAIN, SETTINGS, DETAILS }

data class ItemUi(val entry: Entry, val selected: Boolean) {
    val detected: DetectedFile get() = entry.detected
    val plan: RenamePlan get() = entry.plan
}

data class UiState(
    val hasPermission: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val root: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
    val scanning: Boolean = false,
    val progress: ScanProgress = ScanProgress(),
    val hasScanned: Boolean = false,
    /** The visible rows: after search, filter and sort. */
    val items: List<ItemUi> = emptyList(),
    val totalFound: Int = 0,
    val query: String = "",
    val filter: Filter = Filter.ALL,
    val busy: Boolean = false,
    val canUndo: Boolean = false,
    val diagnostics: String? = null,
    val screen: Screen = Screen.MAIN,
    val detailsId: String? = null,
    val detailsEntry: Entry? = null,
    val details: FileDetails? = null,
    val detailsLoading: Boolean = false,
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
    private val reader = ApkMetadataReader(application)
    private val inspector = ArchiveInspector(reader, File(application.cacheDir, "inspect"))
    private val scanner = FileScanner(inspector)
    private val installedApps = InstalledApps(application)
    private val detailsLoader = DetailsLoader(reader, inspector, installedApps)
    val iconLoader = IconLoader(inspector, reader)

    @Volatile private var detected: List<DetectedFile> = emptyList()
    @Volatile private var entries: List<Entry> = emptyList()
    @Volatile private var installedCodes: Map<String, Long> = emptyMap()
    @Volatile private var selection: Set<String> = emptySet()
    private var scanJob: Job? = null
    private var planJob: Job? = null
    private var detailsJob: Job? = null
    private val lastProgressAt = AtomicLong(0L)

    private val _state = MutableStateFlow(
        settingsRepository.settings.value.let { s ->
            UiState(
                hasPermission = StoragePermission.isGranted(application),
                settings = s,
                root = s.lastRoot?.let { File(it) }
                    ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            )
        },
    )
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

    // ---- navigation -----------------------------------------------------------------------

    fun openSettings() {
        _state.update { it.copy(screen = Screen.SETTINGS) }
    }

    fun back() {
        detailsJob?.cancel()
        _state.update {
            it.copy(screen = Screen.MAIN, detailsId = null, detailsEntry = null, details = null, detailsLoading = false)
        }
    }

    fun openDetails(id: String) {
        val entry = entries.firstOrNull { it.detected.id == id } ?: return
        detailsJob?.cancel()
        _state.update {
            it.copy(screen = Screen.DETAILS, detailsId = id, detailsEntry = entry, details = null, detailsLoading = true)
        }
        detailsJob = viewModelScope.launch {
            val loaded = try {
                detailsLoader.load(entry.detected)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            _state.update { if (it.detailsId == id) it.copy(details = loaded, detailsLoading = false) else it }
        }
    }

    // ---- scanning -------------------------------------------------------------------------

    fun startScan() {
        if (scanJob?.isActive == true) return
        val snapshot = _state.value
        scanJob = viewModelScope.launch {
            _state.update { it.copy(scanning = true, progress = ScanProgress()) }
            try {
                installedCodes = withContext(Dispatchers.IO) { installedApps.versionCodes() }
                val result = scanner.scan(snapshot.root, snapshot.settings.deepScan, snapshot.settings.recursive) { progress ->
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
        entries = emptyList()
        selection = emptySet()
        _state.update { it.copy(items = emptyList(), totalFound = 0, hasScanned = false, root = dir) }
        settingsRepository.update { it.copy(lastRoot = dir.absolutePath) }
    }

    fun onFolderPicked(uri: Uri) {
        val dir = TreeUriPath.toFile(uri)?.takeIf { it.isDirectory }
        if (dir == null) {
            _messages.trySend(UiMessage(R.string.msg_folder_unsupported))
        } else {
            setRoot(dir)
        }
    }

    // ---- search / filter / selection ------------------------------------------------------

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        publish()
    }

    fun setFilter(filter: Filter) {
        _state.update { it.copy(filter = filter) }
        publish()
    }

    fun toggle(id: String) {
        selection = if (id in selection) selection - id else selection + id
        publish()
    }

    /** Applies to the rows currently visible (after search and filter). */
    fun selectAll(all: Boolean) {
        val visible = _state.value.items.map { it.detected.id }
        selection = if (all) selection + visible else selection - visible.toSet()
        publish()
    }

    // ---- renaming -------------------------------------------------------------------------

    fun applyRename() {
        performRename(_state.value.items.filter { it.selected }.map { it.plan })
    }

    fun renameOne(id: String) {
        val entry = entries.firstOrNull { it.detected.id == id } ?: return
        if (entry.plan.status == PlanStatus.READY) performRename(listOf(entry.plan))
    }

    private fun performRename(plans: List<RenamePlan>) {
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
        _state.update { st -> st.copy(detailsId = st.detailsId?.let { moved[it] ?: it }) }
        replan()
    }

    // ---- settings -------------------------------------------------------------------------

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsRepository.update(transform)
    }

    fun resetSettings() {
        settingsRepository.update { AppSettings(lastRoot = it.lastRoot, language = it.language) }
    }

    // ---- derived data ---------------------------------------------------------------------

    private fun duplicateKey(d: DetectedFile): String? {
        val pkg = d.metadata?.packageName
        val code = d.metadata?.versionCode
        return if (pkg != null && code != null) "$pkg|$code" else null
    }

    private fun replan() {
        planJob?.cancel()
        planJob = viewModelScope.launch(Dispatchers.Default) {
            val snapshot = detected
            val plans = try {
                RenamePlanner.plan(snapshot, _state.value.settings.rename)
            } catch (_: Exception) {
                return@launch
            }
            val counts = snapshot.mapNotNull { duplicateKey(it) }.groupingBy { it }.eachCount()
            val codes = installedCodes
            entries = snapshot.zip(plans) { d, p ->
                val meta = d.metadata
                val key = duplicateKey(d)
                Entry(
                    detected = d,
                    plan = p,
                    install = InstallStates.of(meta?.versionCode, meta?.packageName?.let { codes[it] }),
                    duplicate = key != null && (counts[key] ?: 0) > 1,
                )
            }
            publish()
        }
    }

    /** Rebuilds the visible list from [entries] using the current query, filter, sort and selection. */
    private fun publish() {
        val st = _state.value
        val visible = EntryQuery.apply(
            entries, st.query, st.filter, st.settings.sortOrder, st.settings.sortDescending,
        )
        val selected = selection
        val all = entries
        _state.update { cur ->
            cur.copy(
                items = visible.map { ItemUi(it, it.plan.status == PlanStatus.READY && it.detected.id in selected) },
                totalFound = all.size,
                detailsEntry = cur.detailsId?.let { id -> all.firstOrNull { it.detected.id == id } } ?: cur.detailsEntry,
            )
        }
    }
}
