package com.example.campilot

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.campilot.domain.*
import com.example.campilot.navigation.*
import com.example.campilot.ui.CampilotViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

/** Controlled SDK events prove the state gate; this does not simulate an actual native arrival. */
class NavigationHandoffTest {
    @Test fun guidanceEndAllowsManualStayOnlyWhenParked() = runBlocking {
        withContext(Dispatchers.Main) {
            val events = Channel<NavigationEvent>(Channel.BUFFERED)
            val gateway = object : RouteGateway {
                override suspend fun requestRoute(destination: TripPoint, charger: TripPoint?) =
                    Result.success(RouteReceipt(true))
                override fun navigationEvents(destination: TripPoint, charger: TripPoint?) = flow {
                    emit(NavigationEvent.READY)
                    emitAll(events.receiveAsFlow())
                }
            }
            val store = ViewModelStore()
            try {
                val vm = CampilotViewModel(ApplicationProvider.getApplicationContext<Application>(),
                    gateway, TestHomeGateway())
                store.put("navigation", vm)
                withTimeout(10_000) { while(!vm.uiState.value.loaded) delay(20) }
                vm.updatePlan(CampPlan())
                vm.resume()
                withTimeout(10_000) { while(!vm.uiState.value.fresh) delay(20) }
                assertEquals(true, vm.uiState.value.sdk.parked)
                vm.select(demoCampsites[0])
                withTimeout(5_000) { while(vm.uiState.value.homeLoading) delay(20) }
                vm.confirmHome()
                vm.confirmSchedule()
                vm.depart()
                withTimeout(5_000) { while(vm.uiState.value.busy) delay(20) }
                assertEquals(JourneyStage.TO_CAMP, vm.uiState.value.stage)
                assertTrue(vm.uiState.value.travelWindow)
                vm.startStay()
                assertEquals(JourneyStage.TO_CAMP, vm.uiState.value.stage)
                events.send(NavigationEvent.CANCELLED)
                withTimeout(2_000) { while(vm.uiState.value.navigationProblem == null) delay(20) }
                assertFalse(vm.uiState.value.navigationArrived)
                assertTrue(vm.uiState.value.navigationEnded)
                assertFalse(vm.uiState.value.travelWindow)
                assertTrue(vm.uiState.value.canStartStay)
                assertFalse(vm.uiState.value.copy(sdk = vm.uiState.value.sdk.copy(parked = false)).canStartStay)
                assertFalse(vm.uiState.value.copy(sdk = vm.uiState.value.sdk.copy(sampledAt = 0)).canStartStay)
                vm.retryNavigation()
                withTimeout(5_000) { while(vm.uiState.value.busy) delay(20) }
                assertFalse(vm.uiState.value.navigationEnded)
                events.send(NavigationEvent.CANCELLED)
                withTimeout(2_000) { while(!vm.uiState.value.navigationEnded) delay(20) }
                assertFalse(vm.uiState.value.navigationArrived)
                assertFalse(vm.uiState.value.travelWindow)
                vm.startStay()
                assertEquals(JourneyStage.STAY, vm.uiState.value.stage)
            } finally { store.clear(); events.close() }
        }
    }
}
