package io.github.apknamer.domain

/** CPU architectures (ABIs) of an app's native code. */
object Abis {
    private val order = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86", "armeabi", "riscv64")

    /** Maps names such as "arm64_v8a", "config.armeabi_v7a" or "x86-64" to the official ABI name. */
    fun canonical(token: String): String? {
        val t = token.lowercase().replace('_', '-')
        return when {
            t.contains("arm64-v8a") -> "arm64-v8a"
            t.contains("armeabi-v7a") -> "armeabi-v7a"
            t.contains("x86-64") -> "x86_64"
            t.contains("riscv64") -> "riscv64"
            t.contains("armeabi") -> "armeabi"
            t.contains("x86") -> "x86"
            else -> null
        }
    }

    /** From names such as the split APKs of a bundle (`config.arm64_v8a.apk`) or the entries of a JSON list. */
    fun fromNames(names: Iterable<String>): Set<String> =
        names.mapNotNull { canonical(it.substringAfterLast('/')) }.toSet()

    /** From the entry names of an APK: every `lib/<abi>/...` directory. */
    fun fromLibDirs(entryNames: Sequence<String>): Set<String> =
        entryNames.mapNotNull { name ->
            if (name.startsWith("lib/")) canonical(name.split('/').getOrElse(1) { "" }) else null
        }.toSet()

    /** Official names joined with "+", e.g. "arm64-v8a+armeabi-v7a"; null when there are none. */
    fun abiLabel(abis: Set<String>): String? = order.filter { it in abis }.joinToString("+").ifEmpty { null }

    /** Short label: v8, v7, v5, x86, x64; "arm" for several ARM ABIs, "universal" for a mix. */
    fun archLabel(abis: Set<String>): String? {
        val known = order.filter { it in abis }
        return when {
            known.isEmpty() -> null
            known.size == 1 -> short(known[0])
            known.all { it.startsWith("arm") } -> "arm"
            else -> "universal"
        }
    }

    private fun short(abi: String) = when (abi) {
        "arm64-v8a" -> "v8"
        "armeabi-v7a" -> "v7"
        "armeabi" -> "v5"
        "x86_64" -> "x64"
        else -> abi
    }
}
