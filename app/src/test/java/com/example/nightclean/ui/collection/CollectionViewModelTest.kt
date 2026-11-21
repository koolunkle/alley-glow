package com.example.nightclean.ui.collection

import androidx.lifecycle.SavedStateHandle
import com.example.nightclean.domain.model.ShiftRecord
import com.example.nightclean.domain.repository.ShiftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {

    private class FakeShifts(val result: Result<Unit> = Result.success(Unit)) : ShiftRepository {
        var saved: ShiftRecord? = null
        override suspend fun finishShift(record: ShiftRecord): Result<Unit> {
            saved = record
            return result
        }
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repo: ShiftRepository) = CollectionViewModel(
        repo,
        SavedStateHandle(mapOf("shiftId" to "s1", "zoneId" to "G01", "startedAt" to 1_000L, "endedAt" to 5_000L)),
    )

    private val CollectionViewModel.editing get() = uiState.value as CollectionUiState.Editing

    private fun CollectionViewModel.fillWeights(general: String = "12.5", recycle: String = "3", butts: String = "250") {
        changeGeneralKg(general)
        changeRecycleKg(recycle)
        changeButtsG(butts)
    }

    @Test
    fun `봉투 수는 0 아래로 내려가지 않는다`() {
        val vm = viewModel(FakeShifts())
        vm.changeBags(slot = 0, delta = -1)
        vm.changeBags(slot = 2, delta = 1)
        assertEquals(listOf(0, 0, 1, 0), vm.editing.form.bags)
    }

    @Test
    fun `무게가 비어 있으면 저장하지 않고 안내한다`() {
        val repo = FakeShifts()
        val vm = viewModel(repo)
        vm.fillWeights(general = "")
        vm.submit()
        assertNull(repo.saved)
        assertEquals("무게를 숫자로 입력해 주세요. 없으면 0을 적어 주세요.", vm.editing.error)
    }

    @Test
    fun `음수나 상한을 넘는 무게는 받지 않는다`() {
        val repo = FakeShifts()
        val vm = viewModel(repo)
        vm.fillWeights(recycle = "-1")
        vm.submit()
        vm.fillWeights(general = "501")
        vm.submit()
        assertNull(repo.saved)
    }

    @Test
    fun `저장에 성공하면 입력값으로 기록하고 Submitted가 된다`() {
        val repo = FakeShifts()
        val vm = viewModel(repo)
        vm.changeBags(slot = 1, delta = 1)
        vm.fillWeights(general = "12,5")
        vm.changeNote("  ")
        vm.submit()
        assertEquals(
            ShiftRecord("s1", "G01", 1_000L, 5_000L, listOf(0, 1, 0, 0), 12.5, 3.0, 250.0, note = null),
            repo.saved,
        )
        assertEquals(CollectionUiState.Submitted, vm.uiState.value)
    }

    @Test
    fun `저장에 실패하면 입력값을 유지하고 안내한다`() {
        val vm = viewModel(FakeShifts(Result.failure(IllegalStateException())))
        vm.fillWeights()
        vm.submit()
        assertEquals("12.5", vm.editing.form.generalKg)
        assertEquals("저장하지 못했어요. 네트워크를 확인하고 다시 시도해 주세요.", vm.editing.error)
    }
}
