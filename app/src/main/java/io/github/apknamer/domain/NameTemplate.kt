package io.github.apknamer.domain

/**
 * Tiny template engine for file names.
 *
 * - `{token}` is replaced by the matching value (see [TOKENS]); unknown or missing values become empty.
 * - `<...>` marks an optional group: it is dropped entirely if any token inside it is empty.
 *   Example: `{name}< v{version}>< ({code})>` -> `Telegram v10.14.5 (5034)`, or just `Telegram`.
 */
object NameTemplate {
    const val DEFAULT = "{name}< v{version}>< ({code})>"

    val TOKENS = listOf(
        "name", "package", "version", "code", "type", "original",
        "minsdk", "minandroid", "targetsdk", "targetandroid", "abi", "arch",
    )

    private val tokenRegex = Regex("""\{(\w+)\}""")
    private val groupRegex = Regex("""<([^<>]*)>""")
    private val emptyBrackets = Regex("""\(\s*\)|\[\s*\]|\{\s*\}""")
    private val manySpaces = Regex("""\s{2,}""")

    fun unknownTokens(template: String): List<String> =
        tokenRegex.findAll(template)
            .map { it.groupValues[1] }
            .filter { it !in TOKENS }
            .distinct()
            .toList()

    fun render(template: String, values: Map<String, String?>): String {
        val out = StringBuilder()
        var last = 0
        for (match in groupRegex.findAll(template)) {
            out.append(substitute(template.substring(last, match.range.first), values))
            val inner = match.groupValues[1]
            val missing = tokenRegex.findAll(inner).any { values[it.groupValues[1]].isNullOrBlank() }
            if (!missing) out.append(substitute(inner, values))
            last = match.range.last + 1
        }
        out.append(substitute(template.substring(last), values))
        return tidy(out.toString())
    }

    private fun substitute(text: String, values: Map<String, String?>): String =
        tokenRegex.replace(text) { values[it.groupValues[1]].orEmpty() }

    private fun tidy(text: String): String =
        text.replace(emptyBrackets, "")
            .replace(manySpaces, " ")
            .trim()
            .trim('_', '-', '.', ' ')
}
