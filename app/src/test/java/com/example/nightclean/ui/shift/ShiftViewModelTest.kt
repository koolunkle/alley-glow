package com.example.nightclean.ui.shift

import androidx.lifecycle.SavedStateHandle
import com.example.nightclean.domain.model.AdSlot
import com.example.nightclean.domain.model.currentAt
import com.example.nightclean.domain.model.nextAfter
import com.example.nightclean.domain.model.nightHour
import com.example.nightclean.domain.repository.AdSlotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftViewModelTest {

    private val slots = listOf(
        AdSlot("가게 A", 21, 22, "조끼"),
        AdSlot("가게 B", 22, 23, "조끼"),
        AdSlot("가게 C", 24, 26, "조끼"),
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(result: Result<List<AdSlot>>, handle: SavedStateHandle = SavedStateHandle()) =
        ShiftViewModel(object : AdSlotRepository {
            override suspend fun getAdSlots() = result
        }, handle)

    @Test
    fun `불러오기에 성공하면 Success 상태가 된다`() {
        assertEquals(ShiftUiState.Success(slots), viewModel(Result.success(slots)).uiState.value)
    }

    @Test
    fun `불러오기에 실패하면 Error 상태가 된다`() {
        assertTrue(viewModel(Result.failure(IllegalStateException())).uiState.value is ShiftUiState.Error)
    }

    @Test
    fun `저장된 시작 시각을 그대로 쓴다`() {
        val handle = SavedStateHandle(mapOf("startedAt" to 1234L))
        assertEquals(1234L, viewModel(Result.success(slots), handle).startedAt)
    }

    @Test
    fun `청소 전 사진은 2장까지 저장 상태에 담기고 지울 수 있다`() {
        val handle = SavedStateHandle()
        val vm = viewModel(Result.success(slots), handle)
        listOf("a", "b", "c").forEach(vm::addBeforePhoto)
        assertEquals(listOf("a", "b"), vm.beforePhotos.value)
        vm.removeBeforePhoto(0)
        assertEquals(listOf("b"), handle.get<List<String>>("beforePhotos"))
    }

    @Test
    fun `자정을 넘긴 시각도 현재와 다음 슬롯을 찾는다`() {
        assertEquals("가게 A", slots.currentAt(nightHour(21))?.shopName)
        assertEquals("가게 B", slots.nextAfter(nightHour(21))?.shopName)
        assertEquals("가게 C", slots.currentAt(nightHour(1))?.shopName)
        assertNull(slots.nextAfter(nightHour(1)))
        assertNull(slots.currentAt(nightHour(20)))
    }
}
