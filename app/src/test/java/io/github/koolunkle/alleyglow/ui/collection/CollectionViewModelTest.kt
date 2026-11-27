package io.github.koolunkle.alleyglow.ui.collection

import androidx.lifecycle.SavedStateHandle
import io.github.koolunkle.alleyglow.domain.model.ShiftRecord
import io.github.koolunkle.alleyglow.domain.model.Weight
import io.github.koolunkle.alleyglow.domain.repository.ShiftRepository
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

    private fun CollectionViewModel.fillWeights(general: String = "12.5", recycle: String = "3") {
        changeGeneralKg(general)
        changeRecycleKg(recycle)
    }

    @Test
    fun `봉투 수는 0 아래로 내려가지 않는다`() {
        val vm = viewModel(FakeShifts())
        vm.changeBags(slot = 0, delta = -1)
        vm.changeBags(slot = 2, delta = 1)
        assertEquals(listOf(0, 0, 1, 0), vm.editing.form.bags)
    }

    @Test
    fun `저울 없이 봉투 수와 사진만으로 저장한다`() {
        val repo = FakeShifts()
        val vm = viewModel(repo)
        vm.changeBags(slot = 0, delta = 1)
        vm.addPhoto("/cache/photos/bags.jpg")
        vm.submit()
        assertEquals(
            ShiftRecord("s1", "G01", 1_000L, 5_000L, listOf(1, 0, 0, 0), weight = null, note = null, photos = listOf("/cache/photos/bags.jpg")),
            repo.saved,
        )
        assertEquals(CollectionUiState.Submitted, vm.uiState.value)
    }

    @Test
    fun `저울로 쟀으면 무게를 함께 저장한다`() {
        val repo = FakeShifts()
        val vm = viewModel(repo)
        vm.changeBags(slot = 1, delta = 1)
        vm.fillWeights(general = "12,5")
        vm.changeNote("  ")
        vm.addPhoto("/cache/photos/a.jpg")
        vm.submit()
        assertEquals(
            ShiftRecord("s1", "G01", 1_000L, 5_000L, listOf(0, 1, 0, 0), Weight(12.5, 3.0, 0.0), note = null, photos = listOf("/cache/photos/a.jpg")),
            repo.saved,
        )
    }

    @Test
    fun `무게 한 칸만 적으면 빈 칸은 0으로 저장한다`() {
        val repo = FakeShifts()
        val vm = viewModel(repo)
        vm.fillWeights(general = "", recycle = "3")
        vm.addPhoto("/cache/photos/a.jpg")
        vm.submit()
        assertEquals(Weight(0.0, 3.0, 0.0), repo.saved?.weight)
    }

    @Test
    fun `무게가 숫자가 아니면 저장하지 않고 안내한다`() {
        val repo = FakeShifts()
        val vm = viewModel(repo)
        vm.fillWeights(general = "abc")
        vm.addPhoto("/cache/photos/a.jpg")
        vm.submit()
        assertNull(repo.saved)
        assertEquals(CollectionError.WEIGHT, vm.editing.error)
    }

    @Test
    fun `음수나 상한을 넘는 무게는 받지 않는다`() {
        val repo = FakeShifts()
        val vm = viewModel(repo)
        vm.addPhoto("/cache/photos/a.jpg")
        vm.fillWeights(recycle = "-1")
        vm.submit()
        vm.fillWeights(general = "501")
        vm.submit()
        assertNull(repo.saved)
    }

    @Test
    fun `사진이 없으면 저장하지 않고 안내한다`() {
        val repo = FakeShifts()
        val vm = viewModel(repo)
        vm.submit()
        assertEquals(CollectionError.PHOTO, vm.editing.error)
        vm.fillWeights()
        vm.submit()
        assertEquals(CollectionError.PHOTO, vm.editing.error)
        assertNull(repo.saved)
    }

    @Test
    fun `사진은 3장까지 담기고 지울 수 있다`() {
        val vm = viewModel(FakeShifts())
        listOf("a", "b", "c", "d").forEach(vm::addPhoto)
        assertEquals(listOf("a", "b", "c"), vm.editing.form.photos)
        vm.removePhoto(1)
        assertEquals(listOf("a", "c"), vm.editing.form.photos)
    }

    @Test
    fun `저장에 실패하면 입력값을 유지하고 안내한다`() {
        val vm = viewModel(FakeShifts(Result.failure(IllegalStateException())))
        vm.fillWeights()
        vm.addPhoto("/cache/photos/a.jpg")
        vm.submit()
        assertEquals("12.5", vm.editing.form.generalKg)
        assertEquals(listOf("/cache/photos/a.jpg"), vm.editing.form.photos)
        assertEquals(CollectionError.SAVE, vm.editing.error)
    }

    @Test
    fun `청소 전 사진은 근무 화면에서 받고 청소 후 사진은 2장까지 담아 함께 저장한다`() {
        val repo = FakeShifts()
        // 내비게이션이 리스트 인자를 배열로 넣는 경우
        val vm = CollectionViewModel(
            repo,
            SavedStateHandle(
                mapOf("shiftId" to "s1", "zoneId" to "G01", "startedAt" to 1_000L, "endedAt" to 5_000L, "beforePhotos" to arrayOf("b1")),
            ),
        )
        assertEquals(listOf("b1"), vm.beforePhotos)
        listOf("a1", "a2", "a3").forEach(vm::addAfterPhoto)
        vm.removeAfterPhoto(0)
        vm.addPhoto("p1")
        vm.submit()
        assertEquals(listOf("b1"), repo.saved?.beforePhotos)
        assertEquals(listOf("a2"), repo.saved?.afterPhotos)
    }

    @Test
    fun `근무 중에 센 봉투 수를 초기값으로 쓴다`() {
        val vm = CollectionViewModel(
            FakeShifts(),
            SavedStateHandle(
                mapOf("shiftId" to "s1", "zoneId" to "G01", "startedAt" to 1_000L, "endedAt" to 5_000L, "bagsByHour" to intArrayOf(1, 0, 2, 1)),
            ),
        )
        assertEquals(listOf(1, 0, 2, 1), vm.editing.form.bags)
    }
}
