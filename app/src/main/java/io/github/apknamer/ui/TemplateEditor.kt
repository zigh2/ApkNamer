package io.github.apknamer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.apknamer.R
import io.github.apknamer.domain.GroupMode
import io.github.apknamer.domain.NameTemplate
import io.github.apknamer.domain.TemplateChip
import io.github.apknamer.domain.TemplateCodec
import io.github.apknamer.domain.TemplateEditing
import java.util.concurrent.atomic.AtomicLong

private data class EditTarget(val index: Int?, val chip: TemplateChip)

/**
 * The file-name template as draggable tags. Long-press a tag and drag it onto another tag to move it;
 * tap a tag to edit it (change the tag or text, make it optional, move or remove it).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateEditor(template: String, onTemplate: (String) -> Unit) {
    val ids = remember { AtomicLong(1) }
    val chips = remember {
        mutableStateListOf<TemplateChip>().apply { addAll(TemplateCodec.decode(template) { ids.getAndIncrement() }) }
    }
    var editing by remember { mutableStateOf<EditTarget?>(null) }

    fun commit(next: List<TemplateChip>) {
        chips.clear()
        chips.addAll(next)
        onTemplate(TemplateCodec.encode(next))
    }

    // The template can change from outside (preset, reset, raw text editing): rebuild the tags.
    LaunchedEffect(template) {
        if (TemplateCodec.encode(chips.toList()) != template) {
            chips.clear()
            chips.addAll(TemplateCodec.decode(template) { ids.getAndIncrement() })
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.template_label), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.tpl_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChipFlow(
            chips = chips.toList(),
            onMove = { from, to -> commit(TemplateEditing.move(chips.toList(), from, to)) },
            onTap = { index -> editing = EditTarget(index, chips[index]) },
        )
        Text(
            stringResource(R.string.tpl_groups_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(R.string.tpl_add_tags), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            NameTemplate.TOKENS.forEach { token ->
                AssistChip(
                    onClick = { commit(chips.toList() + TemplateChip(ids.getAndIncrement(), token)) },
                    label = { Text(tokenLabel(token)) },
                )
            }
            AssistChip(
                onClick = { editing = EditTarget(null, TemplateChip(ids.getAndIncrement(), null, " ")) },
                label = { Text(stringResource(R.string.tpl_add_text)) },
            )
        }
    }

    editing?.let { target ->
        ChipDialog(
            target = target,
            count = chips.size,
            onDismiss = { editing = null },
            onApply = { draft, mode ->
                val list = chips.toList()
                val index = target.index
                val emptyText = !draft.isToken && draft.text.isEmpty()
                when {
                    emptyText && index == null -> Unit
                    emptyText && index != null -> commit(list.filterIndexed { i, _ -> i != index })
                    else -> {
                        val next = if (index == null) list + draft else list.toMutableList().also { it[index] = draft }
                        val at = index ?: (next.size - 1)
                        commit(if (mode != null) TemplateEditing.setGroup(next, at, mode) else next)
                    }
                }
                editing = null
            },
            onMove = { delta ->
                target.index?.let { commit(TemplateEditing.move(chips.toList(), it, it + delta)) }
                editing = null
            },
            onRemove = {
                target.index?.let { index -> commit(chips.toList().filterIndexed { i, _ -> i != index }) }
                editing = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(chips: List<TemplateChip>, onMove: (Int, Int) -> Unit, onTap: (Int) -> Unit) {
    val bounds = remember { mutableStateMapOf<Long, Rect>() }
    var dragId by remember { mutableStateOf<Long?>(null) }
    var hoverId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var startCenter by remember { mutableStateOf(Offset.Zero) }
    val latestChips by rememberUpdatedState(chips)
    val latestOnMove by rememberUpdatedState(onMove)
    val parities = remember(chips) { groupParities(chips) }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        chips.forEachIndexed { index, chip ->
            key(chip.id) {
                val currentIndex by rememberUpdatedState(index)
                ChipView(
                    chip = chip,
                    parity = parities[index],
                    highlighted = hoverId == chip.id,
                    dragging = dragId == chip.id,
                    modifier = Modifier
                        .onGloballyPositioned { bounds[chip.id] = it.boundsInRoot() }
                        .zIndex(if (dragId == chip.id) 1f else 0f)
                        .graphicsLayer {
                            if (dragId == chip.id) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                            }
                        }
                        .pointerInput(chip.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragId = chip.id
                                    hoverId = null
                                    dragOffset = Offset.Zero
                                    startCenter = bounds[chip.id]?.center ?: Offset.Zero
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount
                                    val point = startCenter + dragOffset
                                    hoverId = bounds.entries
                                        .firstOrNull { it.key != chip.id && it.value.contains(point) }
                                        ?.key
                                },
                                onDragEnd = {
                                    val target = hoverId
                                    val list = latestChips
                                    val from = list.indexOfFirst { it.id == chip.id }
                                    val to = list.indexOfFirst { it.id == target }
                                    dragId = null
                                    hoverId = null
                                    dragOffset = Offset.Zero
                                    if (from >= 0 && to >= 0) latestOnMove(from, to)
                                },
                                onDragCancel = {
                                    dragId = null
                                    hoverId = null
                                    dragOffset = Offset.Zero
                                },
                            )
                        }
                        .clickable { onTap(currentIndex) },
                )
            }
        }
    }
}

@Composable
private fun ChipView(chip: TemplateChip, parity: Int?, highlighted: Boolean, dragging: Boolean, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    val token = chip.token
    val known = token == null || token in NameTemplate.TOKENS
    val container = when {
        !known -> c.errorContainer
        chip.isToken -> c.primaryContainer
        else -> c.surfaceVariant
    }
    val content = when {
        !known -> c.onErrorContainer
        chip.isToken -> c.onPrimaryContainer
        else -> c.onSurfaceVariant
    }
    val borderColor: Color? = when {
        highlighted -> c.primary
        parity == 0 -> c.tertiary
        parity == 1 -> c.secondary
        else -> null
    }
    val label = when {
        token == null -> chip.text.replace(' ', '·').ifEmpty { "·" }
        known -> tokenLabel(token)
        else -> "{" + token + "}"
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = content,
        shadowElevation = if (dragging) 8.dp else 0.dp,
        border = borderColor?.let { BorderStroke(if (highlighted) 3.dp else 2.dp, it) },
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = if (chip.isToken) FontFamily.Default else FontFamily.Monospace,
        )
    }
}

/** Alternating 0 / 1 for consecutive optional groups (so neighbours get different outline colors); null when not in a group. */
private fun groupParities(chips: List<TemplateChip>): List<Int?> {
    var run = -1
    var last: Int? = null
    return chips.map { chip ->
        val g = chip.group
        if (g == null) {
            last = null
            null
        } else {
            if (g != last) {
                run++
                last = g
            }
            run % 2
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipDialog(
    target: EditTarget,
    count: Int,
    onDismiss: () -> Unit,
    onApply: (TemplateChip, GroupMode?) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val chip = target.chip
    val index = target.index
    var token by remember(chip.id) { mutableStateOf(chip.token) }
    var text by remember(chip.id) { mutableStateOf(chip.text) }
    // null = keep the chip's current visibility
    var mode by remember(chip.id) { mutableStateOf<GroupMode?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (index == null) R.string.tpl_new_text else R.string.tpl_edit_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (token != null) {
                    Text(stringResource(R.string.tpl_tag_label), style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NameTemplate.TOKENS.forEach { t ->
                            FilterChip(selected = token == t, onClick = { token = t }, label = { Text(tokenLabel(t)) })
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = TemplateEditing.cleanText(it) },
                        label = { Text(stringResource(R.string.tpl_text_label)) },
                    )
                }
                Text(stringResource(R.string.tpl_group_label), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        GroupMode.ALWAYS to R.string.tpl_group_always,
                        GroupMode.OWN to R.string.tpl_group_own,
                        GroupMode.WITH_PREVIOUS to R.string.tpl_group_join,
                    ).forEach { (m, label) ->
                        FilterChip(
                            selected = mode == m || (mode == null && m == GroupMode.ALWAYS && chip.group == null),
                            onClick = { mode = m },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
                if (index != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onMove(-1) }, enabled = index > 0) {
                            Text(stringResource(R.string.tpl_earlier))
                        }
                        TextButton(onClick = { onMove(1) }, enabled = index < count - 1) {
                            Text(stringResource(R.string.tpl_later))
                        }
                        TextButton(onClick = onRemove) {
                            Text(stringResource(R.string.tpl_remove), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(chip.copy(token = token, text = text), mode) }) {
                Text(stringResource(R.string.tpl_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun tokenLabel(token: String): String = when (token) {
    "name" -> stringResource(R.string.tok_name)
    "package" -> stringResource(R.string.tok_package)
    "version" -> stringResource(R.string.tok_version)
    "code" -> stringResource(R.string.tok_code)
    "type" -> stringResource(R.string.tok_type)
    "original" -> stringResource(R.string.tok_original)
    "minsdk" -> stringResource(R.string.tok_minsdk)
    "minandroid" -> stringResource(R.string.tok_minandroid)
    "targetsdk" -> stringResource(R.string.tok_targetsdk)
    "targetandroid" -> stringResource(R.string.tok_targetandroid)
    "abi" -> stringResource(R.string.tok_abi)
    "arch" -> stringResource(R.string.tok_arch)
    else -> "{" + token + "}"
}
