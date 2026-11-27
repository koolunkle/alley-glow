package io.github.koolunkle.alleyglow.ui.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.ui.common.InlineNotice
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import io.github.koolunkle.alleyglow.ui.theme.cardSurface

/** 위치 수집 동의(메모: 근무 중에만 수집, 동의 화면 1개). 근무마다 눌러야 기록이 시작됨 */
@Composable
internal fun TrackingConsent(permissionDenied: Boolean, onStart: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface()
            .padding(start = Spacing.l, end = Spacing.m, top = Spacing.m, bottom = Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.shift_consent_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.shift_consent_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onStart, contentPadding = PaddingValues(horizontal = Spacing.l), modifier = Modifier.height(48.dp)) {
                Text(stringResource(R.string.shift_consent_start), style = MaterialTheme.typography.labelMedium)
            }
        }
        if (permissionDenied) {
            InlineNotice(stringResource(R.string.shift_consent_denied), MaterialTheme.colorScheme.background)
        }
    }
}
