package io.github.koolunkle.alleyglow.ui.shift

import androidx.lifecycle.SavedStateHandle
import io.github.koolunkle.alleyglow.domain.model.AdSlot
import io.github.koolunkle.alleyglow.domain.model.CheckinResult
import io.github.koolunkle.alleyglow.domain.model.GeoPoint
import io.github.koolunkle.alleyglow.domain.model.bagSlotOf
import io.github.koolunkle.alleyglow.domain.model.currentAt
import io.github.koolunkle.alleyglow.domain.model.nextAfter
import io.github.koolunkle.alleyglow.domain.model.nightHour
import io.github.koolunkle.alleyglow.domain.repository.AdSlotRepository
import io.github.koolunkle.alleyglow.domain.repository.CheckinRepository
import io.github.koolunkle.alleyglow.domain.repository.TrackingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

    private class FakeTracking : TrackingRepository {
        var started: Triple<String, String, Long>? = null
        override val isTracking = MutableStateFlow(false)
        override fun start(shiftId: String, zoneId: String, startedAt: Long) { started = Triple(shiftId, zoneId, startedAt) }
        override fun stop() {}
    }

    private class FakeCheckins(var count: Result<Int> = Result.success(0)) : CheckinRepository {
        override suspend fun checkIn(shiftId: String, zoneId: String, startedAt: Long, shopCode: String, location: GeoPoint) =
            Result.success<CheckinResult>(CheckinResult.UnknownShop(shopCode))
        override suspend fun countCheckins(shiftId: String) = count
    }

    private fun viewModel(
        result: Result<List<AdSlot>>,
        handle: SavedStateHandle = SavedStateHandle(),
        tracking: TrackingRepository = FakeTracking(),
        checkins: CheckinRepository = FakeCheckins(),
    ) = ShiftViewModel(object : AdSlotRepository {
        override suspend fun getAdSlots() = result
    }, checkins, tracking, handle)

    @Test
    fun `봉투는 누른 시각의 시간대 칸에 쌓이고 마지막 것을 되돌린다`() {
        val vm = viewModel(Result.success(slots))
        vm.addBag(19)
        vm.addBag(21)
        vm.addBag(21)
        vm.addBag(23)
        assertEquals(listOf(1, 0, 2, 1), vm.bagsByHour())
        vm.undoBag()
        assertEquals(listOf(1, 0, 2, 0), vm.bagsByHour())
    }

    @Test
    fun `체크인 수는 다시 셀 때 갱신되고 실패하면 이전 값을 유지한다`() {
        val checkins = FakeCheckins(Result.success(2))
        val vm = viewModel(Result.success(slots), checkins = checkins)
        vm.refreshCheckins()
        assertEquals(2, vm.checkinCount.value)
        checkins.count = Result.failure(IllegalStateException())
        vm.refreshCheckins()
        assertEquals(2, vm.checkinCount.value)
    }

    @Test
    fun `봉투 시간대 칸은 19시 전은 첫 칸, 23시 이후와 새벽은 마지막 칸이다`() {
        assertEquals(0, bagSlotOf(15))
        assertEquals(0, bagSlotOf(19))
        assertEquals(3, bagSlotOf(22))
        assertEquals(3, bagSlotOf(23))
        assertEquals(3, bagSlotOf(1))
    }

    @Test
    fun `GPS 기록은 이 근무의 id와 구역, 시작 시각으로 시작한다`() {
        val tracking = FakeTracking()
        val vm = viewModel(Result.success(slots), SavedStateHandle(mapOf("startedAt" to 1234L, "shiftId" to "s1")), tracking)
        vm.startTracking("G07")
        assertEquals(Triple("s1", "G07", 1234L), tracking.started)
    }

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
