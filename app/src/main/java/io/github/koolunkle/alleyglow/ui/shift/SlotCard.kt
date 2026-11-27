package io.github.koolunkle.alleyglow.ui.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.domain.model.AdSlot
import io.github.koolunkle.alleyglow.ui.theme.Spacing

/** 지금 광고(작은 연두 라벨 + 가게명)와 다음 광고 한 줄을 한 카드에 */
@Composable
internal fun SlotCard(current: AdSlot?, next: AdSlot?) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        if (current == null && next == null) {
            Text(stringResource(R.string.shift_slots_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        current?.let { slot ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    stringResource(R.string.shift_slot_now, slot.startHour % 24, slot.endHour % 24),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(horizontal = Spacing.s, vertical = 2.dp),
                )
                Text(slot.shopName, style = MaterialTheme.typography.headlineSmall)
                Text(slot.placement, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        next?.let { slot ->
            if (current != null) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.shift_slot_next, slot.startHour % 24, slot.endHour % 24),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(96.dp),
                )
                Text(slot.shopName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
        }
    }
}
