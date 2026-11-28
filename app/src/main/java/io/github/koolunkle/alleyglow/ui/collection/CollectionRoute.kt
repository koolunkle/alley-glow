package io.github.koolunkle.alleyglow.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.koolunkle.alleyglow.R
import io.github.koolunkle.alleyglow.domain.model.GeoPoint
import io.github.koolunkle.alleyglow.domain.model.HOURLY_WAGE_WON
import io.github.koolunkle.alleyglow.domain.model.MAX_BEFORE_AFTER_PHOTOS
import io.github.koolunkle.alleyglow.domain.model.MAX_PHOTOS
import io.github.koolunkle.alleyglow.domain.model.NIGHT_SLOT_HOURS
import io.github.koolunkle.alleyglow.domain.model.ShiftTrack
import io.github.koolunkle.alleyglow.domain.model.ZoneShare
import io.github.koolunkle.alleyglow.domain.model.estimateWage
import io.github.koolunkle.alleyglow.ui.common.InlineNotice
import io.github.koolunkle.alleyglow.ui.common.PhotoRow
import io.github.koolunkle.alleyglow.ui.common.SectionCard
import io.github.koolunkle.alleyglow.ui.common.SubTopBar
import io.github.koolunkle.alleyglow.ui.theme.AlleyGlowTheme
import io.github.koolunkle.alleyglow.ui.theme.Spacing
import io.github.koolunkle.alleyglow.ui.theme.TouchHeight
import io.github.koolunkle.alleyglow.ui.theme.cardSurface

@Composable
fun CollectionRoute(
    onSubmitted: () -> Unit,
    onBack: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val track by viewModel.track.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) {
        if (uiState is CollectionUiState.Submitted) onSubmitted()
    }
    (uiState as? CollectionUiState.Editing)?.let { editing ->
        CollectionScreen(
            uiState = editing,
            workedMinutes = (viewModel.endedAt - viewModel.startedAt) / 60_000,
            track = track,
            beforePhotoCount = viewModel.beforePhotos.size,
            onBack = onBack,
            onChangeBags = viewModel::changeBags,
            onGeneralKgChange = viewModel::changeGeneralKg,
            onRecycleKgChange = viewModel::changeRecycleKg,
            onNoteChange = viewModel::changeNote,
            onAddPhoto = viewModel::addPhoto,
            onRemovePhoto = viewModel::removePhoto,
            onAddAfterPhoto = viewModel::addAfterPhoto,
            onRemoveAfterPhoto = viewModel::removeAfterPhoto,
            onSubmit = { viewModel.submit() },
            onConfirmZeroBags = { viewModel.submit(zeroBagsConfirmed = true) },
            onDismissZeroBags = viewModel::dismissZeroBags,
        )
    }
}

@Composable
fun CollectionScreen(
    uiState: CollectionUiState.Editing,
    workedMinutes: Long,
    track: ShiftTrack?,
    /** 근무 화면에서 찍은 청소 전 사진 수(후 사진 안내용) */
    beforePhotoCount: Int,
    onBack: () -> Unit,
    onChangeBags: (slot: Int, delta: Int) -> Unit,
    onGeneralKgChange: (String) -> Unit,
    onRecycleKgChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onAddPhoto: (String) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onAddAfterPhoto: (String) -> Unit,
    onRemoveAfterPhoto: (Int) -> Unit,
    onSubmit: () -> Unit,
    onConfirmZeroBags: () -> Unit,
    onDismissZeroBags: () -> Unit,
) {
    val form = uiState.form
    val enabled = !uiState.isSubmitting
    if (uiState.confirmZeroBags) {
        AlertDialog(
            onDismissRequest = onDismissZeroBags,
            title = { Text(stringResource(R.string.collection_zero_bags_title)) },
            text = { Text(stringResource(R.string.collection_zero_bags_body)) },
            confirmButton = { TextButton(onClick = onConfirmZeroBags) { Text(stringResource(R.string.collection_zero_bags_confirm)) } },
            dismissButton = { TextButton(onClick = onDismissZeroBags) { Text(stringResource(R.string.collection_zero_bags_edit)) } },
        )
    }
    Scaffold(
        topBar = { SubTopBar(stringResource(R.string.collection_title), onBack) },
        bottomBar = {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = Spacing.l, vertical = Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                uiState.error?.let {
                    InlineNotice(
                        stringResource(
                            when (it) {
                                CollectionError.WEIGHT -> R.string.collection_error_weight
                                CollectionError.PHOTO -> R.string.collection_error_photo
                                CollectionError.SAVE -> R.string.collection_error_save
                            },
                        ),
                    )
                }
                Button(onClick = onSubmit, enabled = enabled, modifier = Modifier.fillMaxWidth().height(TouchHeight)) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.collection_submit), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.l, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            WorkSummary(workedMinutes, track?.zoneShare())
            track?.let { TrackCard(it) }

            SectionCard(stringResource(R.string.collection_bags), required = true, hint = stringResource(R.string.collection_bags_hint)) {
                NIGHT_SLOT_HOURS.forEachIndexed { slot, hour ->
                    BagStepper(stringResource(R.string.slot_hours, hour, (hour + 1) % 24), form.bags[slot], enabled) { delta ->
                        onChangeBags(slot, delta)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.collection_bags_total), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.shift_bags_value, form.bags.sum()), style = MaterialTheme.typography.titleMedium)
                }
            }

            SectionCard(
                stringResource(R.string.collection_photos),
                required = true,
                hint = stringResource(if (form.weighed) R.string.collection_photos_hint_weighed else R.string.collection_photos_hint),
            ) {
                PhotoRow(form.photos, MAX_PHOTOS, stringResource(R.string.collection_photos), enabled, onAddPhoto, onRemovePhoto)
            }

            SectionCard(
                stringResource(R.string.collection_after_photos),
                required = false,
                hint = if (beforePhotoCount > 0) {
                    stringResource(R.string.collection_after_photos_hint, beforePhotoCount)
                } else {
                    stringResource(R.string.collection_after_photos_hint_none)
                },
            ) {
                PhotoRow(form.afterPhotos, MAX_BEFORE_AFTER_PHOTOS, stringResource(R.string.collection_after_photos), enabled, onAddAfterPhoto, onRemoveAfterPhoto)
            }

            // 시급제라 무게는 수당과 무관. 저울을 썼을 때만 적음(상인 리포트 kg), 비우면 저울 없이 저장
            SectionCard(stringResource(R.string.collection_weight), required = false, hint = stringResource(R.string.collection_weight_hint)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    WeightField(stringResource(R.string.collection_weight_general), form.generalKg, enabled, onGeneralKgChange, Modifier.weight(1f))
                    WeightField(stringResource(R.string.collection_weight_recycle), form.recycleKg, enabled, onRecycleKgChange, Modifier.weight(1f))
                }
            }

            SectionCard(stringResource(R.string.collection_note), required = false, hint = stringResource(R.string.collection_note_hint)) {
                OutlinedTextField(
                    value = form.note,
                    onValueChange = onNoteChange,
                    enabled = enabled,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 근무 시간과 시급제 예상 수당(수거량과 무관), GPS를 기록했으면 구역 안 비율 */
@Composable
private fun WorkSummary(workedMinutes: Long, zoneShare: ZoneShare?) {
    Column(
        Modifier
            .fillMaxWidth()
            .cardSurface()
            .padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.collection_worked), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.collection_worked_value, workedMinutes / 60, workedMinutes % 60), style = MaterialTheme.typography.headlineMedium)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.collection_wage), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.collection_wage_basis, HOURLY_WAGE_WON.won()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(estimateWage(workedMinutes).won(), style = MaterialTheme.typography.titleLarge)
        }
        zoneShare?.let { share ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.collection_zone_share), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.collection_zone_share_caption, share.total, share.inside),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(stringResource(R.string.collection_zone_share_value, share.percent), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun BagStepper(label: String, count: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val minusDesc = stringResource(R.string.collection_bag_minus_desc, label)
    val plusDesc = stringResource(R.string.collection_bag_plus_desc, label)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        FilledTonalButton(
            onClick = { onChange(-1) },
            enabled = enabled && count > 0,
            contentPadding = PaddingValues(0.dp),
            colors = stepperColors(),
            modifier = Modifier.size(TouchHeight).semantics { contentDescription = minusDesc },
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text(
            "$count",
            modifier = Modifier.widthIn(min = 48.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )
        FilledTonalButton(
            onClick = { onChange(1) },
            enabled = enabled,
            contentPadding = PaddingValues(0.dp),
            colors = stepperColors(),
            modifier = Modifier.size(TouchHeight).semantics { contentDescription = plusDesc },
        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

/** 카드 안 −/+: 어두운 바탕, 비활성은 더 흐리게 */
@Composable
private fun stepperColors() = ButtonDefaults.filledTonalButtonColors(
    containerColor = MaterialTheme.colorScheme.background,
    disabledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
)

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

private fun Long.won() = "%,d원".format(this)

@Preview
@Composable
private fun CollectionScreenPreview() {
    AlleyGlowTheme {
        CollectionScreen(
            uiState = CollectionUiState.Editing(
                form = CollectionForm(bags = listOf(1, 2, 1, 0), generalKg = "12.5", recycleKg = "3"),
            ),
            workedMinutes = 232,
            track = ShiftTrack(
                GeoPoint(37.5336, 126.9921),
                List(40) { GeoPoint(37.5336 + 0.0008 * kotlin.math.sin(it / 6.0), 126.9921 + 0.00002 * it * it / 10) },
            ),
            beforePhotoCount = 1,
            onBack = {},
            onChangeBags = { _, _ -> }, onGeneralKgChange = {}, onRecycleKgChange = {},
            onNoteChange = {}, onAddPhoto = {}, onRemovePhoto = {},
            onAddAfterPhoto = {}, onRemoveAfterPhoto = {}, onSubmit = {},
            onConfirmZeroBags = {}, onDismissZeroBags = {},
        )
    }
}
