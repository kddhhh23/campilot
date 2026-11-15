package com.example.campilot

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ai.pleos.playground.navi.data.*
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.junit.Test

/** Read-only diagnostic for the already requested mock campsite. No private destination is printed. */
class PleosRouteSnapshotTest {
    @Test fun inspectActiveMockRoute() = runBlocking {
        withContext(Dispatchers.Main) {
            val helper = NaviHelper(ApplicationProvider.getApplicationContext<Context>())
            val destination = CompletableDeferred<DestinationInfo>()
            val waypoints = CompletableDeferred<List<WaypointInfo>>()
            val state = CompletableDeferred<RouteStateInfo>()
            val listener = object : NaviHelperEventListener {
                override fun onDestinationInfo(destinationInfo: DestinationInfo) { destination.complete(destinationInfo) }
                override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) { waypoints.complete(waypointInfoList) }
                override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) { state.complete(routeStateInfo) }
            }
            try {
                withTimeout(8_000) {
                    suspendCancellableCoroutine<Unit> { c ->
                        helper.initialize({ if(c.isActive) c.resume(Unit) },
                            { if(c.isActive) c.resumeWithException(it) })
                    }
                    helper.addListener(listener)
                    helper.getDestinationInfo(); helper.getWaypointInfo(); helper.getRouteStateInfo()
                    val d = destination.await(); val w = waypoints.await(); val s = state.await()
                    println("RouteSDKSnapshot destinationMatches=" +
                        (kotlin.math.abs(d.latitude-37.7)<.001 && kotlin.math.abs(d.longitude-127.2)<.001) +
                        " namedMock=" + d.name.contains("목업") + " waypoints=" + w.size +
                        " routing=" + s.isRouting + " naviMode=" + s.naviMode)
                }
            } finally { helper.removeListener(listener); helper.release() }
        }
    }
}
