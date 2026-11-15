package com.example.campilot

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.campilot.domain.MockTrip
import com.example.campilot.navigation.PleosNavigator
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

/** Requests the same mock campsite in the real native navigation provider. */
class PleosNavigationRouteTest {
    @Test fun actualProviderConfirmsRouteGuidance() = runBlocking {
        withContext(Dispatchers.Main) {
            val navigator = PleosNavigator(ApplicationProvider.getApplicationContext<Context>())
            val receipt = withTimeout(30_000) { navigator.requestRoute(MockTrip.sites[0].point).getOrThrow() }
            assertTrue("Destination and waypoints must match while the native mode is ROUTE_GUIDING", receipt.confirmed)
        }
    }
}
