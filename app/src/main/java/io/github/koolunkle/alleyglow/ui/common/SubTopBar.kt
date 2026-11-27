package io.github.koolunkle.alleyglow.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.ui.theme.Spacing

/** 근무 중 하위 화면(체크인, 근무 종료) 상단. 뒤로 가기 원 버튼 + 화면 이름 */
@Composable
fun SubTopBar(title: String, onBack: () -> Unit) {
    val backLabel = stringResource(R.string.back)
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(44.dp).semantics { contentDescription = backLabel },
        ) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null, modifier = Modifier.padding(10.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}
