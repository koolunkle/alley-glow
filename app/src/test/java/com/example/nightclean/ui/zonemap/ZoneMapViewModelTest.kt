package com.example.nightclean.ui.zonemap

import com.example.nightclean.domain.model.Zone
import com.example.nightclean.domain.repository.ZoneRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ZoneMapViewModelTest {

    private val zones = listOf(Zone("G01", "격자 1", 0.9f), Zone("G02", "격자 2", 0.1f))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(result: Result<List<Zone>>) = ZoneMapViewModel(object : ZoneRepository {
        override suspend fun getZones() = result
    })

    @Test
    fun `불러오기에 성공하면 Success 상태가 된다`() {
        assertEquals(ZoneMapUiState.Success(zones), viewModel(Result.success(zones)).uiState.value)
    }

    @Test
    fun `불러오기에 실패하면 Error 상태가 된다`() {
        val vm = viewModel(Result.failure(IllegalStateException("network")))
        assertEquals(ZoneMapUiState.Error("구역 정보를 불러오지 못했어요. 네트워크를 확인하고 다시 시도해 주세요."), vm.uiState.value)
    }

    @Test
    fun `격자를 고르면 selectedZone이 바뀐다`() {
        val vm = viewModel(Result.success(zones))
        vm.selectZone("G02")
        assertEquals(zones[1], (vm.uiState.value as ZoneMapUiState.Success).selectedZone)
    }

    @Test
    fun `매출은 억·만 원 단위로 줄여 보인다`() {
        assertEquals("45억 원", formatWon(4_493_365_069))
        assertEquals("4.2억 원", formatWon(418_462_829))
        assertEquals("1.0억 원", formatWon(100_000_000))
        assertEquals("9,999만 원", formatWon(99_990_000))
        assertEquals("0만 원", formatWon(0))
    }
}
