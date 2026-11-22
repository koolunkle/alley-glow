package com.example.nightclean.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.nightclean.R
import com.example.nightclean.ui.theme.NightCleanTheme
import com.example.nightclean.ui.theme.Spacing

/**
 * 역할 첫 화면(지도, 상인 리포트) 상단. 왼쪽 서비스 이름, 오른쪽 계정 아이콘 → 이름·역할과 로그아웃 메뉴
 * (로그아웃은 잘못 누르지 않게 메뉴 안에 둠)
 */
@Composable
fun AppTopBar(role: String, name: String?, onSignOut: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
        val menuLabel = stringResource(R.string.account_menu)
        Box {
            Surface(
                onClick = { menuOpen = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp).semantics { contentDescription = "${name ?: role} $menuLabel" },
            ) {
                Icon(
                    painterResource(R.drawable.ic_person),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.s),
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                offset = DpOffset(0.dp, Spacing.s),
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    Modifier.widthIn(min = 200.dp).padding(horizontal = Spacing.l, vertical = Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(name ?: role, style = MaterialTheme.typography.titleSmall)
                    if (name != null) {
                        Text(role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sign_out), style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_logout), contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    onClick = {
                        menuOpen = false
                        onSignOut()
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun AppTopBarPreview() {
    NightCleanTheme { Surface { AppTopBar(role = "워커", name = "이태원 1조", onSignOut = {}) } }
}
