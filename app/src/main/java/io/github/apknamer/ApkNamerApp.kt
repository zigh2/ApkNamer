package io.github.apknamer

import android.app.Application
import io.github.apknamer.diagnostics.CrashReporter

class ApkNamerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
    }
}
