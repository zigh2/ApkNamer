package io.github.apknamer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.text.format.DateUtils
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.apknamer.R
import io.github.apknamer.data.parse.IconLoader
import io.github.apknamer.domain.DetectedFile
import io.github.apknamer.domain.InstallState

/** App icon loaded lazily; shows the first letter of the app name until (or unless) an icon is available. */
@Composable
fun AppIcon(item: DetectedFile, loader: IconLoader, size: Dp, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(
        initialValue = loader.cached(item),
        item.file.absolutePath,
        item.size,
        item.modified,
    ) {
        if (value == null) value = loader.load(item)
    }
    val shape = RoundedCornerShape(size * 0.24f)
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.size(size).clip(shape),
        )
    } else {
        Box(
            modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (item.metadata?.label ?: item.metadata?.packageName ?: item.file.name).take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
fun Tag(text: String, container: Color, content: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = content,
        modifier = Modifier
            .background(container, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun InstallTag(state: InstallState) {
    val c = MaterialTheme.colorScheme
    when (state) {
        InstallState.UPDATE -> Tag(stringResource(R.string.tag_update), c.tertiaryContainer, c.onTertiaryContainer)
        InstallState.SAME -> Tag(stringResource(R.string.tag_installed), c.surfaceVariant, c.onSurfaceVariant)
        InstallState.DOWNGRADE -> Tag(stringResource(R.string.tag_older), c.errorContainer, c.onErrorContainer)
        else -> Unit
    }
}

/** Replaces the internal-storage prefix with a readable name. */
@Composable
fun rememberPrettyPath(path: String): String {
    val label = stringResource(R.string.root_storage)
    return remember(path, label) {
        @Suppress("DEPRECATION")
        val internal = Environment.getExternalStorageDirectory().absolutePath
        if (path.startsWith(internal)) label + path.removePrefix(internal) else path
    }
}

fun formatSize(context: Context, bytes: Long): String = Formatter.formatShortFileSize(context, bytes)

fun formatDate(context: Context, millis: Long): String =
    DateUtils.formatDateTime(
        context,
        millis,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH,
    )

fun androidVersionName(api: Int): String? = when (api) {
    26 -> "8.0"
    27 -> "8.1"
    28 -> "9"
    29 -> "10"
    30 -> "11"
    31 -> "12"
    32 -> "12L"
    33 -> "13"
    34 -> "14"
    35 -> "15"
    36 -> "16"
    else -> null
}

fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(ClipboardManager::class.java)
    manager?.setPrimaryClip(ClipData.newPlainText("APK Namer", text))
    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
}
