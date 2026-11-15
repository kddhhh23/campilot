package com.example.campilot.navigation

import android.content.Context
import kotlin.math.abs
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.NaviMode
import ai.pleos.playground.navi.constants.WaypointIndex
import ai.pleos.playground.navi.data.*
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import com.example.campilot.domain.TripPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Each request has its own listener lifetime; late results cannot complete a later request. */
data class RouteReceipt(val confirmed: Boolean)

interface RouteGateway {
    suspend fun requestRoute(destination: TripPoint, charger: TripPoint? = null): Result<RouteReceipt>
    fun navigationEvents(destination: TripPoint, charger: TripPoint? = null): Flow<NavigationEvent> = flowOf(NavigationEvent.READY)
    suspend fun showNavigation(): Result<Unit> = Result.success(Unit)
}

class PleosNavigator(private val context: Context) : RouteGateway {
    private val lock = Mutex()
    @Volatile private var activeHelper: NaviHelper? = null

    override fun navigationEvents(destination: TripPoint, charger: TripPoint?): Flow<NavigationEvent> = callbackFlow {
        val helper = NaviHelper(context.applicationContext)
        val gate = NavigationArrivalGate(destination)
        val waypointGate = charger?.let { NavigationArrivalGate(it) }
        val listener = object : NaviHelperEventListener {
            override fun onRouteStarted(routeStartInfo: RouteStartInfo) { gate.reset(); waypointGate?.reset() }
            override fun onRoutingCompleted() { helper.getDestinationInfo(); helper.getWaypointInfo() }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onRouteEnded() { onRoutingCompleted() }
            override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
                if (routeStateInfo.naviMode == NaviMode.ROUTE_GUIDING) { helper.getDestinationInfo(); helper.getWaypointInfo() }
            }
            override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) {
                waypointGate?.reset()
                waypointInfoList.forEach { point ->
                    if (charger != null && kotlin.math.abs(point.latitude - charger.latitude) < .001 &&
                        kotlin.math.abs(point.longitude - charger.longitude) < .001)
                        waypointGate?.updateDestination(point.latitude, point.longitude)
                }
            }
            override fun onDestinationChanged() {
                gate.reset()
                helper.getDestinationInfo()
            }
            override fun onDestinationInfo(destinationInfo: DestinationInfo) {
                gate.updateDestination(destinationInfo.latitude, destinationInfo.longitude)
            }
            override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
                if (gate.accepts(destinationArrivedInfo.name)) trySend(NavigationEvent.DESTINATION_ARRIVED)
            }
            override fun onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo) {
                if (waypointArrivedInfo.index == WaypointIndex.FIRST &&
                    waypointGate?.accepts(waypointArrivedInfo.name) == true)
                    trySend(NavigationEvent.WAYPOINT_ARRIVED)
            }
            override fun onRouteCancelled() {
                gate.reset()
                trySend(NavigationEvent.CANCELLED)
            }
        }
        try {
            helper.connect()
            helper.addListener(listener)
            activeHelper = helper
            send(NavigationEvent.READY)
            awaitClose { helper.removeListener(listener) }
        } finally {
            if (activeHelper === helper) activeHelper = null
            helper.removeListener(listener)
            helper.release()
        }
    }

    override suspend fun showNavigation(): Result<Unit> {
        val shared = activeHelper
        val helper = shared ?: NaviHelper(context.applicationContext)
        return try {
            if (shared == null) helper.connect()
            helper.requestNavigationScreen(NavigationScreenRequest.Foreground)
            Result.success(Unit)
        } catch (_: TimeoutCancellationException) {
            Result.failure(IllegalStateException("내비 화면 연결 시간이 초과됐어요."))
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { Result.failure(e)
        } finally { if (shared == null) helper.release() }
    }

    private suspend fun NaviHelper.connect() {
        withTimeout(8_000) {
            suspendCancellableCoroutine<Unit> { c ->
                initialize({ if (c.isActive) c.resume(Unit) },
                    { if (c.isActive) c.resumeWithException(it) })
            }
        }
    }
    override suspend fun requestRoute(destination: TripPoint, charger: TripPoint?): Result<RouteReceipt> = lock.withLock {
        val shared = activeHelper
        val helper = shared ?: NaviHelper(context.applicationContext)
        val outcome = CompletableDeferred<Unit>()
        var matchedDestination = false
        var matchedWaypoint = false
        var routing = false
        fun confirm() {
            if (matchedDestination && matchedWaypoint && routing) outcome.complete(Unit)
        }
        val listener = object : NaviHelperEventListener {
            override fun onDestinationInfo(destinationInfo: DestinationInfo) {
                matchedDestination = abs(destinationInfo.latitude - destination.latitude) < .001 &&
                    abs(destinationInfo.longitude - destination.longitude) < .001
                confirm()
            }
            override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) {
                matchedWaypoint = if (charger == null) waypointInfoList.isEmpty() else waypointInfoList.any {
                    abs(it.latitude - charger.latitude) < .001 && abs(it.longitude - charger.longitude) < .001
                }
                confirm()
            }
            override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
                // Provider 3.0.4 reports isRouting=false even while ROUTE_GUIDING.
                routing = routeStateInfo.naviMode == NaviMode.ROUTE_GUIDING
                confirm()
            }
            override fun onNavigationStatus(status: NavigationStatus) {
                routing = status == NavigationStatus.ROUTE_DRIVE
                if (routing) { helper.getDestinationInfo(); helper.getWaypointInfo() }
                confirm()
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onRouteEnded() { onRoutingCompleted() }
            override fun onRoutingCompleted() {
                helper.getDestinationInfo()
                helper.getWaypointInfo()
                helper.getRouteStateInfo()
            }
            override fun onError(errorCode: NaviErrorCode) {
                outcome.completeExceptionally(IllegalStateException("내비게이션 오류: " + errorCode))
            }
        }
        try {
            if (shared == null) helper.connect()
            helper.addListener(listener)
            val route = RouteInfo(latitude = destination.latitude, longitude = destination.longitude,
                poiName = destination.name, poiId = "", address = "시연용 좌표", poiSubId = "")
            if (charger == null) helper.requestRoute(route)
            else helper.requestRouteWithWaypoints(RouteWithWaypointsRequest(
                destination = route, waypoints = listOf(RequestWaypointInfo(
                    latitude = charger.latitude, longitude = charger.longitude, poiName = charger.name,
                    waypointIndex = WaypointIndex.FIRST, poiId = "", address = "시연용 경유 좌표", poiSubId = ""
                ))))
            val confirmed = withTimeoutOrNull(20_000) {
                coroutineScope {
                    val query = launch {
                        while (isActive) {
                            delay(1_000)
                            helper.getDestinationInfo()
                            helper.getWaypointInfo()
                            helper.getRouteStateInfo()
                        }
                    }
                    try { outcome.await(); true } finally { query.cancel() }
                }
            }
            // Successful submission is distinct from confirmed guidance. UI requires user confirmation.
            Result.success(RouteReceipt(confirmed == true))
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("내비게이션 응답 시간 초과 · 경로 생성 확인 안 됨"))
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { Result.failure(e)
        } finally {
            outcome.cancel()
            helper.removeListener(listener)
            if (shared == null) helper.release()
        }
    }
}
