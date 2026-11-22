package com.example.nightclean.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nightclean.domain.model.BAG_SLOT_HOURS
import com.example.nightclean.domain.model.BUTT_REWARD_WON_PER_GRAM
import com.example.nightclean.domain.model.HOURLY_WAGE_WON
import com.example.nightclean.domain.model.KG_PER_BAG_ESTIMATE
import com.example.nightclean.domain.model.MAX_BEFORE_AFTER_PHOTOS
import com.example.nightclean.domain.model.MAX_PHOTOS
import com.example.nightclean.domain.model.estimateIncome
import com.example.nightclean.domain.model.estimateKgFromBags
import com.example.nightclean.ui.common.PhotoRow
import com.example.nightclean.ui.theme.NightCleanTheme

@Composable
fun CollectionRoute(
    onSubmitted: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        if (uiState is CollectionUiState.Submitted) onSubmitted()
    }
    (uiState as? CollectionUiState.Editing)?.let { editing ->
        CollectionScreen(
            uiState = editing,
            workedMinutes = (viewModel.endedAt - viewModel.startedAt) / 60_000,
            beforePhotoCount = viewModel.beforePhotos.size,
            onChangeBags = viewModel::changeBags,
            onGeneralKgChange = viewModel::changeGeneralKg,
            onRecycleKgChange = viewModel::changeRecycleKg,
            onButtsGChange = viewModel::changeButtsG,
            onNoteChange = viewModel::changeNote,
            onWeighedChange = viewModel::setWeighed,
            onAddPhoto = viewModel::addPhoto,
            onRemovePhoto = viewModel::removePhoto,
            onAddAfterPhoto = viewModel::addAfterPhoto,
            onRemoveAfterPhoto = viewModel::removeAfterPhoto,
            onSubmit = viewModel::submit,
        )
    }
}

@Composable
fun CollectionScreen(
    uiState: CollectionUiState.Editing,
    workedMinutes: Long,
    /** 근무 화면에서 찍은 청소 전 사진 수(후 사진 안내용) */
    beforePhotoCount: Int,
    onChangeBags: (slot: Int, delta: Int) -> Unit,
    onGeneralKgChange: (String) -> Unit,
    onRecycleKgChange: (String) -> Unit,
    onButtsGChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onWeighedChange: (Boolean) -> Unit,
    onAddPhoto: (String) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onAddAfterPhoto: (String) -> Unit,
    onRemoveAfterPhoto: (Int) -> Unit,
    onSubmit: () -> Unit,
) {
    val form = uiState.form
    val enabled = !uiState.isSubmitting
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("오늘 치운 양을 알려주세요", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)

            Section {
                Text("채운 봉투 (100L)", fontWeight = FontWeight.Bold)
                BAG_SLOT_HOURS.forEachIndexed { slot, hour ->
                    BagStepper("$hour–${hour + 1}시", form.bags[slot], enabled) { delta -> onChangeBags(slot, delta) }
                }
            }

            Section {
                Text("무게", fontWeight = FontWeight.Bold)
                Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeighChoice("저울 없음", selected = !form.weighed, enabled, Modifier.weight(1f)) { onWeighedChange(false) }
                    WeighChoice("저울로 쟀어요", selected = form.weighed, enabled, Modifier.weight(1f)) { onWeighedChange(true) }
                }
                if (form.weighed) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeightField("일반 (kg)", form.generalKg, enabled, onGeneralKgChange, Modifier.weight(1f))
                        WeightField("재활용 (kg)", form.recycleKg, enabled, onRecycleKgChange, Modifier.weight(1f))
                        WeightField("꽁초 (g)", form.buttsG, enabled, onButtsGChange, Modifier.weight(1f))
                    }
                } else {
                    val bags = form.bags.sum()
                    Text(
                        "추정 약 ${formatKg(estimateKgFromBags(bags))}kg (봉투 ${bags}장 × ${formatKg(KG_PER_BAG_ESTIMATE)}kg, 가정값)",
                        fontWeight = FontWeight.Bold,
                    )
                    Hint("추정치는 저장하지 않아요. 수거량 보상은 저울로 잰 무게로만 계산돼요.")
                }
            }

            Section {
                val what = if (form.weighed) "저울" else "채운 봉투"
                Text("$what 사진 (필수, 최대 ${MAX_PHOTOS}장)", fontWeight = FontWeight.Bold)
                Hint(if (form.weighed) "봉투를 저울에 올리고 눈금이 보이게 찍어 주세요" else "채운 봉투가 모두 보이게 찍어 주세요")
                PhotoRow(form.photos, MAX_PHOTOS, "$what 사진", enabled, onAddPhoto, onRemovePhoto)
            }

            Section {
                Text("청소 후 사진 (선택, 최대 ${MAX_BEFORE_AFTER_PHOTOS}장)", fontWeight = FontWeight.Bold)
                Hint(
                    if (beforePhotoCount > 0) {
                        "청소 전 사진 ${beforePhotoCount}장과 같은 자리, 같은 각도로 찍어 주세요"
                    } else {
                        "청소 전 사진은 근무 화면에서 찍어요. 같은 자리, 같은 각도로 찍으면 비교하기 좋아요"
                    },
                )
                PhotoRow(form.afterPhotos, MAX_BEFORE_AFTER_PHOTOS, "청소 후 사진", enabled, onAddAfterPhoto, onRemoveAfterPhoto)
            }

            OutlinedTextField(
                value = form.note,
                onValueChange = onNoteChange,
                label = { Text("특이사항 (민원, 위험 상황, 혼잡)") },
                enabled = enabled,
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            val buttsG = if (form.weighed) form.buttsG.toWeight(Double.MAX_VALUE) ?: 0.0 else 0.0
            val income = estimateIncome(workedMinutes, buttsG)
            Section {
                Text("오늘 예상 수입 · 근무 ${workedMinutes / 60}시간 ${workedMinutes % 60}분", fontWeight = FontWeight.Bold)
                IncomeRow("광고비 몫 (시급 ${HOURLY_WAGE_WON.won()})", income.wage)
                IncomeRow("꽁초 보상 (저울 사용·제도 도입 시, 1g당 ${BUTT_REWARD_WON_PER_GRAM}원)", income.buttReward)
                HorizontalDivider()
                IncomeRow("합계", income.total, bold = true)
            }

            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = onSubmit,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("근무 종료하기", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Section(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun BagStepper(label: String, count: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        FilledTonalButton(
            onClick = { onChange(-1) },
            enabled = enabled && count > 0,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(56.dp).semantics { contentDescription = "$label 봉투 하나 빼기" },
        ) { Text("−") }
        Text(
            "$count",
            modifier = Modifier.widthIn(min = 40.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        FilledTonalButton(
            onClick = { onChange(1) },
            enabled = enabled,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(56.dp).semantics { contentDescription = "$label 봉투 하나 더하기" },
        ) { Text("+") }
    }
}

/** 저울 사용 여부 선택(라디오 그룹) */
@Composable
private fun WeighChoice(label: String, selected: Boolean, enabled: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    Row(
        modifier
            .height(56.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

/** 소수점 아래가 0이면 정수로(18.0 → 18) */
private fun formatKg(kg: Double): String = if (kg % 1.0 == 0.0) kg.toLong().toString() else "%.1f".format(kg)

@Composable
private fun WeightField(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text("0") },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun IncomeRow(label: String, won: Long, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(won.won(), fontWeight = if (bold) FontWeight.Black else FontWeight.Normal)
    }
}

private fun Long.won() = "%,d원".format(this)

@Preview
@Composable
private fun CollectionScreenPreview() {
    NightCleanTheme {
        CollectionScreen(
            uiState = CollectionUiState.Editing(
                form = CollectionForm(bags = listOf(1, 2, 1, 0), generalKg = "12.5", recycleKg = "3", buttsG = "250"),
            ),
            workedMinutes = 240,
            beforePhotoCount = 1,
            onChangeBags = { _, _ -> }, onGeneralKgChange = {}, onRecycleKgChange = {}, onButtsGChange = {},
            onNoteChange = {}, onWeighedChange = {}, onAddPhoto = {}, onRemovePhoto = {},
            onAddAfterPhoto = {}, onRemoveAfterPhoto = {}, onSubmit = {},
        )
    }
}
