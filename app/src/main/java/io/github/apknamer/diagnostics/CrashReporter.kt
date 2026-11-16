package io.github.apknamer.diagnostics

import android.app.Application
import android.os.Build
import android.util.Log
import java.io.File
import java.util.Date

/**
 * Saves the stack trace of an uncaught exception to a private file so the next launch can show it.
 * Nothing is sent anywhere; the user decides whether to copy and share the report.
 */
object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { File(app.filesDir, FILE_NAME).writeText(format(app, thread, throwable)) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Returns the saved report (and deletes it), or null when the last session did not crash. */
    fun consume(app: Application): String? {
        val file = File(app.filesDir, FILE_NAME)
        if (!file.exists()) return null
        val text = runCatching { file.readText() }.getOrNull()
        file.delete()
        return text
    }

    private fun format(app: Application, thread: Thread, throwable: Throwable): String {
        val version = runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull()
        return buildString {
            appendLine("APK Namer $version")
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Time: ${Date()}")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(Log.getStackTraceString(throwable))
        }
    }
}
