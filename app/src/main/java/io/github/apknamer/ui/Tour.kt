package io.github.apknamer.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.apknamer.R
import kotlin.math.roundToInt

data class TourStep(val target: String?, @StringRes val title: Int, @StringRes val body: Int)

val TOUR_STEPS = listOf(
    TourStep("location", R.string.tour_1_title, R.string.tour_1_body),
    TourStep("filters", R.string.tour_2_title, R.string.tour_2_body),
    TourStep(null, R.string.tour_3_title, R.string.tour_3_body),
    TourStep("selection", R.string.tour_4_title, R.string.tour_4_body),
    TourStep(null, R.string.tour_5_title, R.string.tour_5_body),
    TourStep("settings", R.string.tour_6_title, R.string.tour_6_body),
)

/** Where each highlightable element currently is on screen, by key. */
val LocalTourBounds = compositionLocalOf<MutableMap<String, Rect>> { mutableMapOf() }

fun Modifier.tourTarget(bounds: MutableMap<String, Rect>, key: String): Modifier =
    onGloballyPositioned { bounds[key] = it.boundsInRoot() }

/**
 * A guided tour that never blocks the app: the highlight is only drawn (it does not consume touches),
 * the card is small and can be dragged out of the way, and the tour can be skipped at any step.
 */
@Composable
fun TourOverlay(step: Int, bounds: Map<String, Rect>, onNext: () -> Unit, onSkip: () -> Unit) {
    val current = TOUR_STEPS.getOrNull(step) ?: return
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var drag by remember(step) { mutableStateOf(Offset.Zero) }
    val target = current.target?.let { bounds[it] }
    val pulse by rememberInfiniteTransition(label = "tour-pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "tour-alpha",
    )
    val color = MaterialTheme.colorScheme.primary
    // Keep the card away from what it is explaining.
    val atTop = target != null && size.height > 0 && target.center.y > size.height / 2f

    Box(
        Modifier.fillMaxSize().onGloballyPositioned {
            origin = it.positionInRoot()
            size = it.size
        },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (target != null) {
                val pad = 4.dp.toPx()
                drawRoundRect(
                    color = color.copy(alpha = pulse),
                    topLeft = Offset(target.left - origin.x - pad, target.top - origin.y - pad),
                    size = Size(target.width + 2 * pad, target.height + 2 * pad),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
        Card(
            modifier = Modifier
                .align(if (atTop) Alignment.TopCenter else Alignment.BottomCenter)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
                .widthIn(max = 360.dp)
                .offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
                .pointerInput(step) {
                    detectDragGestures { change, amount ->
                        change.consume()
                        drag += amount
                    }
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(current.title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.tour_step, step + 1, TOUR_STEPS.size),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(stringResource(current.body), style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.tour_skip)) }
                    Button(onClick = onNext) {
                        Text(stringResource(if (step == TOUR_STEPS.lastIndex) R.string.tour_done else R.string.tour_next))
                    }
                }
            }
        }
    }
}
