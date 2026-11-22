package com.example.nightclean.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nightclean.R
import com.example.nightclean.ui.theme.NightCleanTheme
import com.example.nightclean.ui.theme.Spacing

/** 역할 첫 화면(지도, 상인 리포트) 상단. 왼쪽 서비스 이름, 오른쪽 계정 칩 → 로그아웃(잘못 누르지 않게 메뉴 안에 둠) */
@Composable
fun AppTopBar(account: String, onSignOut: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
        val menuLabel = stringResource(R.string.account_menu)
        Box {
            Surface(
                onClick = { menuOpen = true },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.heightIn(min = 40.dp).semantics { contentDescription = "$account $menuLabel" },
            ) {
                Box(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s), Alignment.Center) {
                    Text("$account ▾", style = MaterialTheme.typography.labelMedium)
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sign_out)) },
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
    NightCleanTheme { Surface { AppTopBar(account = "워커", onSignOut = {}) } }
}
