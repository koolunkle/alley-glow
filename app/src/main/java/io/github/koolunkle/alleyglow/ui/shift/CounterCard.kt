package io.github.koolunkle.alleyglow.ui.shift

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import io.github.koolunkle.alleyglow.ui.theme.TouchHeight

/** 이름, 큰 숫자, 아래쪽 버튼 줄 */
@Composable
internal fun RowScope.CounterCard(label: String, value: String, actions: @Composable () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .padding(Spacing.l),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.weight(1f).height(Spacing.l))
        actions()
    }
}

/** 봉투를 채울 때마다 +1(누른 시각의 시간대 칸에 1장). 두 버튼 같은 높이·모양 */
@Composable
internal fun BagButtons(bagCount: Int, onAdd: () -> Unit, onUndo: () -> Unit) {
    val undoDesc = stringResource(R.string.shift_bag_undo_desc)
    val addDesc = stringResource(R.string.shift_bag_add_desc)
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        FilledTonalButton(
            onClick = onUndo,
            enabled = bagCount > 0,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.weight(1f).height(TouchHeight).semantics { contentDescription = undoDesc },
        ) { Text("−1", style = MaterialTheme.typography.labelLarge) }
        Button(
            onClick = onAdd,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.weight(1f).height(TouchHeight).semantics { contentDescription = addDesc },
        ) { Text("+1", style = MaterialTheme.typography.labelLarge) }
    }
}
