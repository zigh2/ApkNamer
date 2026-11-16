package io.github.apknamer.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(
    primary = Color(0xFF1B6B4A),
    secondaryContainer = Color(0xFFCDE9D9),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF8ED1A5),
    secondaryContainer = Color(0xFF2F4F3E),
)

@Composable
fun ApkNamerTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    pureBlack: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> Dark
        else -> Light
    }
    if (darkTheme && pureBlack) {
        scheme = scheme.copy(background = Color.Black, surface = Color.Black)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
