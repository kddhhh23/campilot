package com.example.campilot

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.campilot.domain.*
import com.example.campilot.navigation.*
import com.example.campilot.ui.CampilotViewModel
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ScheduleStateTest {
    @Test fun changingDepartureRecalculatesBudgetWithoutStartingRoute() = runBlocking {
        withContext(Dispatchers.Main) {
            var routeRequests = 0
            val navigator = object : RouteGateway {
                override suspend fun requestRoute(destination: TripPoint, charger: TripPoint?): Result<RouteReceipt> {
                    routeRequests++
                    return Result.success(RouteReceipt(false))
                }
            }
            val store = ViewModelStore()
            val vm = CampilotViewModel(ApplicationProvider.getApplicationContext<Application>(), navigator, TestHomeGateway())
            store.put("schedule", vm)
            try {
                withTimeout(5_000) { while (!vm.uiState.value.loaded) delay(20) }
                vm.resume()
                withTimeout(10_000) { while (!vm.uiState.value.fresh) delay(20) }
                vm.select(demoCampsites[1])
                withTimeout(5_000) { while(vm.uiState.value.homeLoading) delay(20) }
                assertEquals(JourneyStage.HOME_SETUP, vm.uiState.value.stage)
                assertNull(vm.uiState.value.journeyBudget)
                vm.confirmSchedule()
                assertEquals(JourneyStage.HOME_SETUP, vm.uiState.value.stage)
                vm.confirmHome()
                assertEquals("테스트 귀가 목적지", vm.uiState.value.confirmedHome?.label)
                vm.chooseDepartureTime(9 * 60 + 15)
                vm.confirmSchedule()
                val first = vm.uiState.value
                vm.backToSchedule()
                vm.chooseDepartureTime(10 * 60 + 15)
                vm.confirmSchedule()
                val second = vm.uiState.value
                assertEquals(JourneyStage.BUDGET, second.stage)
                assertEquals(demoCampsites[1], second.selected)
                assertEquals(1.0, second.plan.hours - first.plan.hours, .01)
                assertEquals(1.2, second.journeyBudget!!.required - first.journeyBudget!!.required, .02)
                assertEquals("10:15", second.departureAt!!.toLocalTime().toString())
                assertEquals(0, routeRequests)
            } finally { store.clear() }
        }
    }
}
