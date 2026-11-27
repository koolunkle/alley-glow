package io.github.koolunkle.alleyglow.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 정보 카드 바탕(남색 한 단계 밝은 색 + 큰 모서리). 안쪽 여백은 카드마다 따로 */
@Composable
fun Modifier.cardSurface(): Modifier = background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
