package com.example.nightclean.ui.collection

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nightclean.domain.model.BAG_SLOT_HOURS
import com.example.nightclean.domain.model.BUTT_REWARD_WON_PER_GRAM
import com.example.nightclean.domain.model.HOURLY_WAGE_WON
import com.example.nightclean.domain.model.MAX_SCALE_PHOTOS
import com.example.nightclean.domain.model.estimateIncome
import com.example.nightclean.ui.theme.NightCleanTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CollectionRoute(
    onSubmitted: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        if (uiState is CollectionUiState.Submitted) onSubmitted()
    }
    val context = LocalContext.current
    // 촬영 중 화면이 재생성돼도 저장할 파일 경로를 잃지 않게
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingPhoto ?: return@rememberLauncherForActivityResult
        pendingPhoto = null
        if (saved) viewModel.addScalePhoto(path) else File(path).delete()
    }
    (uiState as? CollectionUiState.Editing)?.let { editing ->
        CollectionScreen(
            uiState = editing,
            workedMinutes = (viewModel.endedAt - viewModel.startedAt) / 60_000,
            onChangeBags = viewModel::changeBags,
            onGeneralKgChange = viewModel::changeGeneralKg,
            onRecycleKgChange = viewModel::changeRecycleKg,
            onButtsGChange = viewModel::changeButtsG,
            onNoteChange = viewModel::changeNote,
            onTakeScalePhoto = {
                // 시스템 카메라로만 촬영(갤러리 선택 불가). 파일은 캐시의 photos/에
                val file = File(File(context.cacheDir, "photos").apply { mkdirs() }, "scale-${System.currentTimeMillis()}.jpg")
                pendingPhoto = file.path
                takePicture.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            },
            onRemoveScalePhoto = viewModel::removeScalePhoto,
            onSubmit = viewModel::submit,
        )
    }
}

@Composable
fun CollectionScreen(
    uiState: CollectionUiState.Editing,
    workedMinutes: Long,
    onChangeBags: (slot: Int, delta: Int) -> Unit,
    onGeneralKgChange: (String) -> Unit,
    onRecycleKgChange: (String) -> Unit,
    onButtsGChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onTakeScalePhoto: () -> Unit,
    onRemoveScalePhoto: (Int) -> Unit,
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeightField("일반 (kg)", form.generalKg, enabled, onGeneralKgChange, Modifier.weight(1f))
                WeightField("재활용 (kg)", form.recycleKg, enabled, onRecycleKgChange, Modifier.weight(1f))
                WeightField("꽁초 (g)", form.buttsG, enabled, onButtsGChange, Modifier.weight(1f))
            }

            Section {
                Text("저울 사진 (필수, 최대 ${MAX_SCALE_PHOTOS}장)", fontWeight = FontWeight.Bold)
                Text(
                    "봉투를 저울에 올리고 눈금이 보이게 찍어 주세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    form.scalePhotos.forEachIndexed { index, path ->
                        PhotoThumb(path, "저울 사진 ${index + 1}", enabled) { onRemoveScalePhoto(index) }
                    }
                    if (form.scalePhotos.size < MAX_SCALE_PHOTOS) {
                        OutlinedButton(
                            onClick = onTakeScalePhoto,
                            enabled = enabled,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(88.dp),
                        ) { Text("촬영\n${form.scalePhotos.size}/${MAX_SCALE_PHOTOS}", textAlign = TextAlign.Center) }
                    }
                }
            }

            OutlinedTextField(
                value = form.note,
                onValueChange = onNoteChange,
                label = { Text("특이사항 (민원, 위험 상황, 혼잡)") },
                enabled = enabled,
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            val income = estimateIncome(workedMinutes, form.buttsG.toWeight(Double.MAX_VALUE) ?: 0.0)
            Section {
                Text("오늘 예상 수입 · 근무 ${workedMinutes / 60}시간 ${workedMinutes % 60}분", fontWeight = FontWeight.Bold)
                IncomeRow("광고비 몫 (시급 ${HOURLY_WAGE_WON.won()})", income.wage)
                IncomeRow("꽁초 보상 (제도 도입 시, 1g당 ${BUTT_REWARD_WON_PER_GRAM}원)", income.buttReward)
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

/** 촬영한 사진 미리보기. 원본을 작게 디코딩(IO 스레드) */
@Composable
private fun PhotoThumb(path: String, label: String, enabled: Boolean, onRemove: () -> Unit) {
    val image by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 16 })?.asImageBitmap()
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(88.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.background)) {
            image?.let { Image(it, contentDescription = label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        TextButton(onClick = onRemove, enabled = enabled) { Text("삭제") }
    }
}

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
            onChangeBags = { _, _ -> }, onGeneralKgChange = {}, onRecycleKgChange = {}, onButtsGChange = {},
            onNoteChange = {}, onTakeScalePhoto = {}, onRemoveScalePhoto = {}, onSubmit = {},
        )
    }
}
