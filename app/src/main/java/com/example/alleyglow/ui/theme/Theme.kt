package com.example.alleyglow.ui.theme

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
    // 보조(tonal) 버튼이 기본 보라로 나오지 않게 남색 계열로
    secondaryContainer = NavyRaised,
    onSecondaryContainer = Ink,
    // 드롭다운·다이얼로그 바탕
    surfaceContainerLowest = Navy,
    surfaceContainerLow = NavySurface,
    surfaceContainer = NavyRaised,
    surfaceContainerHigh = NavyRaised,
    surfaceContainerHighest = NavyRaised,
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
fun AlleyGlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NightColorScheme,
        typography = Typography,
        shapes = NightShapes,
        content = content,
    )
}
