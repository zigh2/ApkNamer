package io.github.apknamer.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Date

/**
 * Android 11+ remembers why each app process ended. This reports unusual endings (native crash,
 * ANR, out of memory, permission change...) once, which also covers failures that no Java
 * exception handler can see.
 */
object ExitReasonReporter {
    private val ordinary = setOf(1 /* EXIT_SELF */, 10 /* USER_REQUESTED */, 11 /* USER_STOPPED */)

    fun lastReport(context: Context): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) describeLatest(context) else null

    @RequiresApi(Build.VERSION_CODES.R)
    private fun describeLatest(context: Context): String? {
        val prefs = context.getSharedPreferences("diagnostics", Context.MODE_PRIVATE)
        val seen = prefs.getLong("last_exit_ts", 0L)
        val manager = context.getSystemService(ActivityManager::class.java) ?: return null
        val latest = runCatching { manager.getHistoricalProcessExitReasons(context.packageName, 0, 1) }
            .getOrNull()?.firstOrNull() ?: return null
        if (latest.timestamp <= seen) return null
        prefs.edit().putLong("last_exit_ts", latest.timestamp).apply()
        if (latest.reason in ordinary) return null

        val trace = if (latest.reason == 6) {
            runCatching { latest.traceInputStream?.bufferedReader()?.use { it.readText().take(4000) } }.getOrNull()
        } else {
            null
        }
        return buildString {
            appendLine("Process exit: ${name(latest.reason)}")
            appendLine("Time: ${Date(latest.timestamp)}")
            appendLine("Status: ${latest.status}, importance: ${latest.importance}")
            latest.description?.let { appendLine("Description: $it") }
            if (latest.reason == 8) {
                appendLine("Android restarts an app when its storage access changes; this is normal.")
            }
            if (!trace.isNullOrBlank()) {
                appendLine()
                append(trace)
            }
        }.trimEnd()
    }

    private fun name(reason: Int) = when (reason) {
        0 -> "UNKNOWN"
        2 -> "SIGNALED (killed by a signal)"
        3 -> "LOW_MEMORY"
        4 -> "CRASH (Java exception)"
        5 -> "CRASH_NATIVE"
        6 -> "ANR (app not responding)"
        7 -> "INITIALIZATION_FAILURE"
        8 -> "PERMISSION_CHANGE"
        9 -> "EXCESSIVE_RESOURCE_USAGE"
        12 -> "DEPENDENCY_DIED"
        13 -> "OTHER"
        14 -> "FREEZER"
        15 -> "PACKAGE_STATE_CHANGE"
        16 -> "PACKAGE_UPDATED"
        else -> "REASON_$reason"
    }
}
