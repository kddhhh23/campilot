package com.example.campilot

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.example.campilot.domain.MockTrip
import com.example.campilot.navigation.PleosNavigator
import com.example.campilot.vehicle.PleosVehicleGateway
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

/** Runs actual IPC against the installed Pleos service, never a fake gateway. */
class PleosSdkIntegrationTest {
    @Test fun officialVehicleSdkReadsEmulatorBattery() = runBlocking {
        withContext(Dispatchers.Main) {
            val gateway = PleosVehicleGateway(ApplicationProvider.getApplicationContext<Context>())
            try {
                val snapshot = gateway.refresh()
                Log.i("CampilotSdkProbe", snapshot.toString())
                assertNotNull(snapshot.message, snapshot.soc)
                assertTrue(snapshot.soc!! in 0.0..100.0)
            } finally { gateway.close() }
        }
    }
    @Test fun officialNaviSdkRequestsMockDestination() = runBlocking {
        withContext(Dispatchers.Main) {
            val result = PleosNavigator(ApplicationProvider.getApplicationContext<Context>())
                .requestRoute(MockTrip.sites[0].point, MockTrip.charger)
            Log.i("CampilotSdkProbe", "Navigation route: " + result)
            assertTrue(result.exceptionOrNull()?.message ?: "SDK request was not submitted", result.isSuccess)
            // Missing provider callbacks remain unconfirmed and require explicit UI confirmation.
            assertNotNull(result.getOrNull())
        }
    }
}
