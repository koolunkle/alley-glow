package com.example.nightclean.ui.shopreport

import com.example.nightclean.domain.model.ShopReport
import com.example.nightclean.domain.repository.ShopReportRepository
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
class ShopReportViewModelTest {

    private val report = ShopReport(1, "가게 A", 11, nights = 2, collectedKg = 16.0, guests = 3, recent = emptyList())

    private class FakeReports(
        val report: Result<ShopReport?>,
        val add: Result<Unit> = Result.success(Unit),
    ) : ShopReportRepository {
        var added = 0
        override suspend fun getReport() = report
        override suspend fun addGuest(shopId: Long) = add.also { if (it.isSuccess) added++ }
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `리포트를 불러오면 Success가 된다`() {
        val vm = ShopReportViewModel(FakeReports(Result.success(report)))
        assertEquals(ShopReportUiState.Success(report), vm.uiState.value)
    }

    @Test
    fun `연결된 가게가 없으면 NoShop이 된다`() {
        assertEquals(ShopReportUiState.NoShop, ShopReportViewModel(FakeReports(Result.success(null))).uiState.value)
    }

    @Test
    fun `손님 기록에 성공하면 손님 수가 1 오른다`() {
        val repo = FakeReports(Result.success(report))
        val vm = ShopReportViewModel(repo)
        vm.addGuest()
        assertEquals(1, repo.added)
        assertEquals(4, (vm.uiState.value as ShopReportUiState.Success).report.guests)
    }

    @Test
    fun `손님 기록에 실패하면 숫자는 그대로 두고 안내한다`() {
        val vm = ShopReportViewModel(FakeReports(Result.success(report), add = Result.failure(IllegalStateException())))
        vm.addGuest()
        val state = vm.uiState.value as ShopReportUiState.Success
        assertEquals(3, state.report.guests)
        assertEquals("손님을 기록하지 못했어요. 다시 눌러 주세요.", state.message)
    }

    @Test
    fun `날짜와 무게를 읽기 쉽게 표시한다`() {
        assertEquals("11/29 (일)", formatNight("2026-11-29"))
        assertEquals("bad", formatNight("bad"))
        assertEquals("16", formatKg(16.0))
        assertEquals("12.5", formatKg(12.5))
    }
}
