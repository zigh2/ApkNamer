package io.github.apknamer.domain

/** Makes arbitrary text safe to use as a file name on Android file systems (ext4, f2fs, FAT, exFAT). */
object FileNameSanitizer {
    private val illegal = Regex("""[\\/:*?"<>|\u0000-\u001F]""")
    private val whitespace = Regex("""\s+""")

    fun sanitize(raw: String, replaceSpaces: Boolean = false, maxBytes: Int = 200): String {
        var s = raw.replace(illegal, "_").replace(whitespace, " ").trim()
        if (replaceSpaces) s = s.replace(' ', '_')
        s = s.trim('.', ' ')
        return truncateUtf8(s, maxBytes)
    }

    private fun truncateUtf8(s: String, maxBytes: Int): String {
        if (s.toByteArray(Charsets.UTF_8).size <= maxBytes) return s
        val out = StringBuilder()
        var bytes = 0
        var i = 0
        while (i < s.length) {
            val cp = s.codePointAt(i)
            val len = String(Character.toChars(cp)).toByteArray(Charsets.UTF_8).size
            if (bytes + len > maxBytes) break
            out.appendCodePoint(cp)
            bytes += len
            i += Character.charCount(cp)
        }
        return out.toString().trimEnd()
    }
}
