package io.github.koolunkle.alleyglow.ui.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import io.github.koolunkle.alleyglow.ui.theme.TouchHeight
import io.github.koolunkle.alleyglow.ui.theme.Warning

/** 근무 상태(연두 점). GPS 기록 중이면 문구에 표시 */
@Composable
internal fun StatusChip(isTracking: Boolean) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Text(
            stringResource(if (isTracking) R.string.shift_status_tracking else R.string.shift_status),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** 긴급 연락. 옅은 주황 칩(터치 영역은 48dp 보장) */
@Composable
internal fun EmergencyChip(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = Warning.copy(alpha = 0.14f), contentColor = Warning) {
        Row(
            Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_call), contentDescription = null, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.shift_emergency), style = MaterialTheme.typography.labelMedium)
        }
    }
}

// shortcut: 112·119만 둠, 운영팀(크루장) 연락처가 정해지면 목록에 추가
@Composable
internal fun EmergencyDialog(onDial: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shift_emergency)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(stringResource(R.string.emergency_body))
                Button(onClick = { onDial("112") }, modifier = Modifier.fillMaxWidth().height(TouchHeight)) {
                    Text(stringResource(R.string.emergency_police), style = MaterialTheme.typography.labelLarge)
                }
                Button(onClick = { onDial("119") }, modifier = Modifier.fillMaxWidth().height(TouchHeight)) {
                    Text(stringResource(R.string.emergency_fire), style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
