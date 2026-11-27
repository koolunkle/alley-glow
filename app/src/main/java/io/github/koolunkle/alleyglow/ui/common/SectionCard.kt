package io.github.koolunkle.alleyglow.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import io.github.koolunkle.alleyglow.ui.theme.cardSurface

/** 입력 묶음 카드: 제목 + 필수/선택 칩 + 한 줄 안내 + 내용. 근무 중·근무 종료 공통 */
@Composable
fun SectionCard(
    title: String,
    required: Boolean? = null,
    hint: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface()
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                required?.let { RequirementChip(it) }
            }
            hint?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}

/** 필수는 연두 글자, 선택은 회색 글자. 카드 바탕보다 어두운 칩 */
@Composable
private fun RequirementChip(required: Boolean) {
    Text(
        stringResource(if (required) R.string.required else R.string.optional),
        style = MaterialTheme.typography.labelSmall,
        color = if (required) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background, CircleShape)
            .padding(horizontal = Spacing.s, vertical = 2.dp),
    )
}
