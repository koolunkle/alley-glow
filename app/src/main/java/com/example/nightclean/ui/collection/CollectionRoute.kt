package com.example.nightclean.ui.collection

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.nightclean.ui.common.PlaceholderScreen

// TODO: CollectionViewModel 추가 (봉투 수, 종류별 무게, 저울 사진, 예상 수입)
@Composable
fun CollectionRoute(onSubmitted: () -> Unit) {
    CollectionScreen(onSubmit = onSubmitted)
}

@Composable
fun CollectionScreen(onSubmit: () -> Unit) {
    PlaceholderScreen(title = "근무 종료", description = "봉투 수, 종류별 무게, 저울 사진, 예상 수입이 들어갈 자리") {
        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) { Text("근무 종료하기") }
    }
}
