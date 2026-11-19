package io.github.apknamer.domain

/**
 * One draggable "tag" of the file-name template: either a token chip (`{name}`) or a piece of plain text.
 * Chips with the same non-null [group] that sit next to each other form one optional `<...>` group.
 */
data class TemplateChip(
    val id: Long,
    /** Token name without braces for a token chip; null for a plain-text chip. */
    val token: String?,
    val text: String = "",
    val group: Int? = null,
) {
    val isToken: Boolean get() = token != null
}

enum class GroupMode { ALWAYS, OWN, WITH_PREVIOUS }

/** Converts between the template string and its chips. `decode(encode(x)) == x` for chips without stray markers. */
object TemplateCodec {
    fun encode(chips: List<TemplateChip>): String {
        val out = StringBuilder()
        var current: Int? = null
        for (chip in chips) {
            val g = chip.group
            if (g != current) {
                if (current != null) out.append('>')
                if (g != null) out.append('<')
                current = g
            }
            out.append(if (chip.token != null) "{" + chip.token + "}" else chip.text)
        }
        if (current != null) out.append('>')
        return out.toString()
    }

    fun decode(template: String, newId: () -> Long): List<TemplateChip> {
        val chips = ArrayList<TemplateChip>()
        val text = StringBuilder()
        var group: Int? = null
        var nextGroup = 1

        fun flush() {
            if (text.isNotEmpty()) {
                chips += TemplateChip(newId(), null, text.toString(), group)
                text.setLength(0)
            }
        }

        var i = 0
        while (i < template.length) {
            val c = template[i]
            when {
                c == '<' && group == null -> {
                    flush()
                    group = nextGroup++
                    i++
                }
                c == '>' && group != null -> {
                    flush()
                    group = null
                    i++
                }
                c == '{' -> {
                    val end = template.indexOf('}', i + 1)
                    val name = if (end > i + 1) template.substring(i + 1, end) else ""
                    if (name.isNotEmpty() && name.all { it.isLetterOrDigit() || it == '_' }) {
                        flush()
                        chips += TemplateChip(newId(), name, "", group)
                        i = end + 1
                    } else {
                        i++ // stray brace: ignored
                    }
                }
                c == '<' || c == '>' || c == '}' -> i++ // stray marker: ignored
                else -> {
                    text.append(c)
                    i++
                }
            }
        }
        flush()
        return chips
    }
}

/** Pure edit operations used by the drag-and-drop template editor. */
object TemplateEditing {
    /** Characters that would be re-interpreted as template syntax are not allowed in text chips. */
    fun cleanText(s: String): String = s.filterNot { it == '<' || it == '>' || it == '{' || it == '}' }

    /** Moves the chip at [from] so that it takes the place of the chip at [to]. */
    fun move(chips: List<TemplateChip>, from: Int, to: Int): List<TemplateChip> {
        if (from !in chips.indices || to !in chips.indices || from == to) return chips
        val list = chips.toMutableList()
        val moved = list.removeAt(from)
        list.add(to, moved)
        val before = list.getOrNull(to - 1)?.group
        val after = list.getOrNull(to + 1)?.group
        // Dropped between two members of the same group: it joins that group.
        if (before != null && before == after) list[to] = moved.copy(group = before)
        return list
    }

    fun setGroup(chips: List<TemplateChip>, index: Int, mode: GroupMode): List<TemplateChip> {
        if (index !in chips.indices) return chips
        val list = chips.toMutableList()
        val freshId = (chips.mapNotNull { it.group }.maxOrNull() ?: 0) + 1
        when (mode) {
            GroupMode.ALWAYS -> list[index] = list[index].copy(group = null)
            GroupMode.OWN -> list[index] = list[index].copy(group = freshId)
            GroupMode.WITH_PREVIOUS -> {
                val previous = list.getOrNull(index - 1)
                when {
                    previous == null -> list[index] = list[index].copy(group = freshId)
                    previous.group != null -> list[index] = list[index].copy(group = previous.group)
                    else -> {
                        list[index - 1] = previous.copy(group = freshId)
                        list[index] = list[index].copy(group = freshId)
                    }
                }
            }
        }
        return list
    }
}
