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

/** Actual Pleos battery IPC, controlled route response for lifecycle boundary coverage. */
class RouteLifecycleTest {
    private suspend fun ready(gateway: RouteGateway, store: ViewModelStore): CampilotViewModel {
        val vm = CampilotViewModel(ApplicationProvider.getApplicationContext<Application>(), gateway, TestHomeGateway())
        store.put("test", vm)
        withTimeout(10_000) { while (!vm.uiState.value.loaded) delay(20) }
        vm.updatePlan(CampPlan())
        vm.resume()
        withTimeout(10_000) { while (!vm.uiState.value.fresh) delay(20) }
        vm.select(demoCampsites[0])
        withTimeout(5_000) { while(vm.uiState.value.homeLoading) delay(20) }
        vm.confirmHome()
        vm.confirmSchedule()
        return vm
    }
    @Test fun backgroundKeepsRouteButUnconfirmedNeedsUserAction() = runBlocking {
        withContext(Dispatchers.Main) {
            val response = CompletableDeferred<Result<RouteReceipt>>()
            val gateway = object : RouteGateway {
                override suspend fun requestRoute(destination: TripPoint, charger: TripPoint?) = response.await()
            }
            val store = ViewModelStore()
            try {
                val vm = ready(gateway, store)
                vm.depart()
                assertTrue(vm.uiState.value.busy)
                vm.background()
                response.complete(Result.success(RouteReceipt(false)))
                withTimeout(2_000) { while(vm.uiState.value.busy) delay(20) }
                assertEquals(JourneyStage.BUDGET, vm.uiState.value.stage)
                assertEquals(JourneyStage.TO_CAMP, vm.uiState.value.pendingRoute)
                assertFalse(vm.uiState.value.applying)
                vm.confirmRoute()
                assertEquals(JourneyStage.TO_CAMP, vm.uiState.value.stage)
            } finally { store.clear() }
        }
    }
    @Test fun changingDestinationDiscardsBothKindsOfLateResponse() = runBlocking {
        withContext(Dispatchers.Main) {
            for (confirmed in listOf(true, false)) {
                val response = CompletableDeferred<Result<RouteReceipt>>()
                val gateway = object : RouteGateway {
                    override suspend fun requestRoute(destination: TripPoint, charger: TripPoint?) = response.await()
                }
                val store = ViewModelStore()
                try {
                    val vm = ready(gateway, store)
                    vm.depart()
                    vm.backToRecommendations()
                    assertFalse("Back must stop waiting for the previous route", vm.uiState.value.busy)
                    vm.select(demoCampsites[1])
                    response.complete(Result.success(RouteReceipt(confirmed)))
                    delay(100)
                    assertEquals(JourneyStage.SCHEDULE, vm.uiState.value.stage)
                    assertEquals(demoCampsites[1], vm.uiState.value.selected)
                    assertNull(vm.uiState.value.pendingRoute)
                } finally { store.clear() }
            }
        }
    }
    @Test fun resetDiscardsLateNavigationResult() = runBlocking {
        withContext(Dispatchers.Main) {
            val response = CompletableDeferred<Result<RouteReceipt>>()
            val gateway = object : RouteGateway {
                override suspend fun requestRoute(destination: TripPoint, charger: TripPoint?) = response.await()
            }
            val store = ViewModelStore()
            try {
                val vm = ready(gateway, store)
                vm.depart()
                vm.reset()
                response.complete(Result.success(RouteReceipt(true)))
                delay(100)
                assertEquals(JourneyStage.RECOMMEND, vm.uiState.value.stage)
                assertNull(vm.uiState.value.pendingRoute)
                assertFalse(vm.uiState.value.busy)
            } finally { store.clear() }
        }
    }
}
