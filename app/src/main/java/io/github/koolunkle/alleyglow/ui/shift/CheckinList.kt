package io.github.koolunkle.alleyglow.ui.shift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.domain.model.CheckedInShop
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import io.github.koolunkle.alleyglow.ui.theme.cardSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 이번 근무에 체크인한 가게와 시각(시각 순) */
@Composable
internal fun CheckinList(checkedIn: List<CheckedInShop>) {
    val time = SimpleDateFormat("HH:mm", Locale.KOREA)
    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface()
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(stringResource(R.string.shift_checkin_list), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        checkedIn.forEach { shop ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(shop.shopName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(time.format(Date(shop.checkedAt)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
