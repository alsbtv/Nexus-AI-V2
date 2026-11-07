package com.nexus.ai.ui.theme
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightAccentBlue, background = LightBackground, surface = LightSurface,
    onPrimary = LightBackground, onBackground = LightTextPrimary, onSurface = LightTextPrimary,
    surfaceVariant = LightUserBubble, onSurfaceVariant = LightTextSecondary, outline = LightDivider
)
private val DarkColorScheme = darkColorScheme(
    primary = DarkAccentBlue, background = DarkBackground, surface = DarkSurface,
    onPrimary = DarkBackground, onBackground = DarkTextPrimary, onSurface = DarkTextPrimary,
    surfaceVariant = DarkUserBubble, onSurfaceVariant = DarkTextSecondary, outline = DarkDivider
)

@Composable
fun NexusTheme(
    darkTheme: Boolean = true, // إجبار الوضع الداكن
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // شريط حالة شفاف
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            // إخفاء الأيقونات البيضاء في الوضع الداكن
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme

            // جعل التطبيق يمتد خلف شريط الحالة (Edge-to-Edge)
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
