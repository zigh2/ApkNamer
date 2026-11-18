package io.github.apknamer.domain

import java.io.File

/** Pure logic that turns detected packages into a conflict-free rename plan. */
object RenamePlanner {
    private const val MAX_NAME_BYTES = 200

    fun plan(
        items: List<DetectedFile>,
        options: RenameOptions,
        exists: (File) -> Boolean = { it.exists() },
    ): List<RenamePlan> {
        val claimed = HashSet<String>()
        return items.map { item ->
            val meta = item.metadata
            if (item.type == PackageType.UNKNOWN || meta == null || !meta.isUsable) {
                return@map RenamePlan(item.file, null, PlanStatus.UNREADABLE)
            }

            val original = item.file.nameWithoutExtension
            val rendered = NameTemplate.render(
                options.template,
                mapOf(
                    "name" to (meta.label ?: meta.packageName),
                    "package" to meta.packageName,
                    "version" to meta.versionName,
                    "code" to meta.versionCode?.toString(),
                    "type" to item.type.extension.uppercase(),
                    "original" to original,
                    "minsdk" to meta.minSdk?.toString(),
                    "minandroid" to meta.minSdk?.let { AndroidVersions.name(it) },
                    "targetsdk" to meta.targetSdk?.toString(),
                    "targetandroid" to meta.targetSdk?.let { AndroidVersions.name(it) },
                    "abi" to Abis.abiLabel(meta.abis),
                    "arch" to Abis.archLabel(meta.abis),
                ),
            )
            val base = FileNameSanitizer
                .sanitize(rendered.ifBlank { original }, options.replaceSpaces, MAX_NAME_BYTES)
                .ifBlank { original }
            val ext = targetExtension(item.file, item.type, options.fixExtension)
            fun nameFor(suffix: String) = if (ext.isEmpty()) "$base$suffix" else "$base$suffix.$ext"

            val parent = item.file.absoluteFile.parentFile
            var n = 1
            var candidate = File(parent, nameFor(""))
            if (candidate.name == item.file.name) {
                claimed += key(item.file)
                return@map RenamePlan(item.file, null, PlanStatus.UNCHANGED)
            }
            while (isTaken(candidate, item.file, claimed, exists)) {
                n++
                candidate = File(parent, nameFor(" ($n)"))
            }
            if (candidate.name == item.file.name) {
                claimed += key(item.file)
                return@map RenamePlan(item.file, null, PlanStatus.UNCHANGED)
            }
            claimed += key(candidate)
            RenamePlan(item.file, candidate, PlanStatus.READY)
        }
    }

    private fun targetExtension(file: File, type: PackageType, fix: Boolean): String {
        val current = file.extension
        return if (fix && type != PackageType.UNKNOWN && current.lowercase() !in type.aliases) {
            type.extension
        } else {
            current
        }
    }

    private fun key(file: File) = file.absolutePath.lowercase()

    private fun isTaken(
        candidate: File,
        source: File,
        claimed: Set<String>,
        exists: (File) -> Boolean,
    ): Boolean {
        if (key(candidate) in claimed) return true
        val samePath = candidate.absolutePath.equals(source.absolutePath, ignoreCase = true)
        return !samePath && exists(candidate)
    }
}
