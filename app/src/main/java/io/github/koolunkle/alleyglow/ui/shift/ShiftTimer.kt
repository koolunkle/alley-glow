package io.github.koolunkle.alleyglow.ui.shift

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun ShiftTimer(startedAt: Long, now: Long, zoneName: String) {
    val minutes = ((now - startedAt) / 60_000).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.s), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.shift_elapsed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("%02d:%02d".format(minutes / 60, minutes % 60), style = MaterialTheme.typography.displayMedium)
        Text(
            stringResource(R.string.shift_started, SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(startedAt)), zoneName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
