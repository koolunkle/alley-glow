package com.example.nightclean.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

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

// 모서리: 배지·입력 12, 카드 20, 큰 카드 24
private val NightShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun NightCleanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NightColorScheme,
        typography = Typography,
        shapes = NightShapes,
        content = content,
    )
}
