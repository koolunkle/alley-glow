package com.example.nightclean.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nightclean.R
import com.example.nightclean.ui.theme.Spacing
import com.example.nightclean.ui.theme.Warning

/** 입력 오류·실패 안내 한 줄. 글자는 본문색, 주황은 경고 아이콘에만. container는 놓이는 바탕보다 한 단계 다른 색 */
@Composable
fun InlineNotice(text: String, container: Color = MaterialTheme.colorScheme.surfaceVariant) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(container, MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = Warning, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
