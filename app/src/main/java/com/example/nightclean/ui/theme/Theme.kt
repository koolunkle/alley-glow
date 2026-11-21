package com.example.nightclean.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// 다크 테마 단일 사용. 브랜드 색 유지를 위해 다이내믹 컬러 미사용
private val NightColorScheme = darkColorScheme(
    primary = Lime,
    onPrimary = Navy,
    background = Navy,
    onBackground = Ink,
    surface = Navy,
    onSurface = Ink,
    surfaceVariant = NavySurface,
    onSurfaceVariant = InkMuted,
    outline = NavyLine,
    error = Warning,
    onError = Navy,
)

@Composable
fun NightCleanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NightColorScheme,
        typography = Typography,
        content = content,
    )
}
