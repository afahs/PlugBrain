package app.plugbrain.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFBEB0FF),
    primaryContainer = Color(0xFF342B56),
    onPrimaryContainer = Color(0xFFE8DFFF),
    secondary = Color(0xFF9CD6C2),
    surface = Color(0xFF1E1D26),
    onSurface = Color(0xFFF1EDF7),
    onPrimary = Color(0xFF26164D),
    onSecondary = Primary100,
    background = Color(0xFF13121A),
    onBackground = Color(0xFFF1EDF7),
    error = Error600,
    outline = Neutral200,
    outlineVariant = Neutral500,
    surfaceVariant = Color(0xFF2C2938),
    onSurfaceVariant = Neutral300,
    tertiary = Success300,
    onTertiary = White,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF6250A3),
    primaryContainer = Color(0xFFEDE7FA),
    onPrimaryContainer = Color(0xFF30234F),
    secondary = Color(0xFF326B58),
    surface = White,
    onSurface = Color(0xFF252230),
    onPrimary = White,
    onSecondary = Primary900,
    background = Color(0xFFF7F6FA),
    onBackground = Color(0xFF252230),
    error = Error500,
    outline = Neutral300,
    outlineVariant = Neutral200,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral400,
    tertiary = Success500,
    onTertiary = White,
  )

@Composable
fun MathlockAppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}
