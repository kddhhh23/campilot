package com.example.campilot.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campilot.data.PlanStore
import com.example.campilot.domain.*
import com.example.campilot.navigation.PleosNavigator
import com.example.campilot.navigation.RouteGateway
import com.example.campilot.navigation.NavigationEvent
import com.example.campilot.navigation.NavigationSessionService
import com.example.campilot.navigation.HomeGateway
import com.example.campilot.navigation.PleosHomeGateway
import com.example.campilot.vehicle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.ceil
import java.time.ZonedDateTime

data class CampilotUiState(
    val plan: CampPlan = CampPlan(),
    val selected: Campsite = demoCampsites[0],
    val phase: TripPhase = TripPhase.BEFORE_TRAVEL,
    val stage: JourneyStage = JourneyStage.RECOMMEND,
    val recommendationsReady: Boolean = false,
    val homeCandidate: HomeDestination? = null,
    val confirmedHome: HomeDestination? = null,
    val homeLoading: Boolean = false,
    val homeError: Boolean = false,
    val homeStatus: String = "내비게이션에 등록된 집을 확인해주세요.",
    val scheduleNow: ZonedDateTime = ZonedDateTime.now(),
    val departureMinuteOfDay: Int = 9 * 60,
    val departureAt: ZonedDateTime? = null,
    val sdk: PleosSnapshot = PleosSnapshot(),
    val energy: Double = 0.0,
    val elapsed: Double = 0.0,
    val temperature: Double = 22.0,
    val power: Double = 1.2,
    val applying: Boolean = false,
    val running: Boolean = false,
    val busy: Boolean = false,
    val message: String = "목적지를 고르면 충전부터 귀가까지 함께 준비해요",
    val routeMessage: String = "",
    val routeError: Boolean = false,
    val pendingRoute: JourneyStage? = null,
    val fault: String? = null,
    val history: List<Float> = emptyList(),
    val loaded: Boolean = false,
    val viaCharger: Boolean = false,
    val charged: Boolean = false,
    val returning: Boolean = false,
    val chargeTarget: Double = 0.0,
    val navigationArrived: Boolean = false,
    val navigationEnded: Boolean = false,
    val navigationProblem: String? = null
) {
    val travelWindow: Boolean get() = stage in listOf(JourneyStage.TO_CHARGER, JourneyStage.TO_CAMP, JourneyStage.HOME) &&
        !busy && pendingRoute == null && !navigationArrived && navigationProblem == null
    val remaining get() = (plan.hours - elapsed).coerceAtLeast(0.0)
    val site get() = MockTrip.sites.first { it.campsite.id == selected.id }
    val homeKm: Double? get() = confirmedHome?.let { HomeDistanceEstimate.km(site.point, it.point) }
    val returnChargerKm: Double get() = HomeDistanceEstimate.km(site.point, MockTrip.charger)
    val chargerToHomeKm: Double? get() = confirmedHome?.let { HomeDistanceEstimate.km(MockTrip.charger, it.point) }
    val observedSoc: Double? get() = sdk.soc
    val fresh: Boolean get() = (sdk.soc != null && sdk.sampledAt > 0 &&
        SystemClock.elapsedRealtime() - sdk.sampledAt in 0..10_000)
    val budget: Budget get() = if (!fresh) Budget.Invalid("차량 배터리 조회 필요") else
        EnergyCalculator.calculate(plan, selected.distance, phase, energy, remaining, power)
    val journeyBudget: JourneyBudget? get() {
        if (!fresh) return null
        val returnKm = homeKm ?: return null
        val distance = when {
            phase == TripPhase.STAY -> 0.0
            charged || stage == JourneyStage.CHARGING -> (selected.distance - MockTrip.chargerKm).coerceAtLeast(0.0)
            else -> selected.distance
        }
        return JourneyBudget.calculate(plan.copy(hours = remaining, totalPower = power),
            distance, returnKm, energy)
    }
    val canStartStay get() = stage == JourneyStage.TO_CAMP && !busy && pendingRoute == null &&
        (navigationArrived || navigationEnded) && fresh && sdk.parked == true
    val canSetTemperature get() = phase == TripPhase.STAY && !busy && !applying &&
        ControlGuard.canControl(sdk.parked, sdk.temperature,
            sdk.writable, sdk.sampledAt, SystemClock.elapsedRealtime())
}

class CampilotViewModel(app: Application, private val navigator: RouteGateway,
    private val homes: HomeGateway = PleosHomeGateway(app)) : AndroidViewModel(app) {
    constructor(app: Application) : this(app, PleosNavigator(app))
    private val store = PlanStore(app)
    private var vehicle: PleosVehicleGateway? = null
    private val mutable = MutableStateFlow(CampilotUiState())
    val uiState = mutable.asStateFlow()
    private var command: Job? = null
    private var clock: Job? = null
    private var poll: Job? = null
    private var scheduleTicker: Job? = null
    private var route: Job? = null
    private var navigationMonitor: Job? = null
    private var homeJob: Job? = null
    private var registrationJob: Job? = null
    private var homeGeneration = 0
    private var registrationPending = false
    private var generation = 0
    private var routeGeneration = 0
    private var foreground = false

    init {
        viewModelScope.launch {
            val p = try { store.load() } catch (_: java.io.IOException) { CampPlan() }
            mutable.update { it.copy(plan = p, temperature = p.temperature, loaded = true) }
        }
    }

    fun updatePlan(p: CampPlan) {
        if (EnergyCalculator.calculate(p, 0.0) is Budget.Invalid || p.hours <= 0) return
        reset()
        mutable.update { it.copy(plan = p, energy = (it.sdk.soc ?: 0.0) * p.capacity / 100,
            temperature = it.sdk.temperature ?: p.temperature, history = emptyList()) }
        viewModelScope.launch {
            try { store.save(p) } catch (_: java.io.IOException) { message("설정 저장 실패 · 이번 여정에는 적용됨") }
        }
    }

    fun requestRecommendations() {
        if (mutable.value.stage != JourneyStage.RECOMMEND) return
        mutable.update { it.copy(recommendationsReady = true) }
    }

    fun select(site: Campsite) {
        invalidateRoute()
        if (mutable.value.phase == TripPhase.STAY) reset()
        stop()
        mutable.update { it.copy(selected = site,
            stage = if (it.confirmedHome == null) JourneyStage.HOME_SETUP else JourneyStage.SCHEDULE,
            pendingRoute = null, routeMessage = "", scheduleNow = ZonedDateTime.now()) }
        if (mutable.value.confirmedHome == null) refreshHome() else startScheduleTicker()
    }

    fun refreshHome() {
        if (mutable.value.stage != JourneyStage.HOME_SETUP) return
        homeJob?.cancel()
        val token = ++homeGeneration
        mutable.update { it.copy(homeLoading = true, homeError = false, homeCandidate = null,
            homeStatus = "내비게이션에서 집 정보를 불러오고 있어요.") }
        homeJob = viewModelScope.launch {
            val result = homes.readHome()
            if (token != homeGeneration) return@launch
            mutable.update { it.copy(homeLoading = false, homeCandidate = result.getOrNull(),
                homeError = result.isFailure, homeStatus = when {
                    result.isFailure -> result.exceptionOrNull()?.message ?: "집 정보를 불러오지 못했어요."
                    result.getOrNull() == null -> "내비게이션에 집 주소를 등록해주세요."
                    else -> "이 주소로 귀가하는 것이 맞나요?"
                }) }
        }
    }

    fun confirmHome() {
        val s = mutable.value
        if (s.stage != JourneyStage.HOME_SETUP || s.homeLoading) return
        val home = s.homeCandidate ?: return
        mutable.update { it.copy(confirmedHome = home, stage = JourneyStage.SCHEDULE,
            scheduleNow = ZonedDateTime.now()) }
        startScheduleTicker()
    }

    fun editHome() {
        if (mutable.value.stage !in listOf(JourneyStage.SCHEDULE, JourneyStage.BUDGET)) return
        invalidateRoute()
        scheduleTicker?.cancel()
        mutable.update { it.copy(stage = JourneyStage.HOME_SETUP, confirmedHome = null) }
        refreshHome()
    }

    fun registerHome() {
        if (mutable.value.stage != JourneyStage.HOME_SETUP || mutable.value.homeLoading) return
        homeJob?.cancel()
        val token = ++homeGeneration
        registrationPending = true
        mutable.update { it.copy(homeLoading = true, homeError = false, homeStatus = "내비게이션 집 등록 화면을 요청하고 있어요.") }
        registrationJob = viewModelScope.launch {
            val result = homes.openHomeRegistration()
            if (token != homeGeneration) return@launch
            mutable.update { it.copy(homeLoading = false, homeError = result.isFailure,
                homeStatus = if (result.isSuccess) "내비게이션에서 집을 등록한 뒤 돌아와 다시 불러오세요."
                    else result.exceptionOrNull()?.message ?: "집 등록 화면을 열지 못했어요.") }
        }
    }

    fun chooseDepartureTime(minuteOfDay: Int) {
        if (mutable.value.stage != JourneyStage.SCHEDULE || minuteOfDay !in 0 until 1440) return
        mutable.update { it.copy(departureMinuteOfDay = minuteOfDay) }
    }

    fun confirmSchedule() {
        val s = mutable.value
        if (s.stage != JourneyStage.SCHEDULE) return
        if (s.confirmedHome == null) return message("귀가할 집을 먼저 확인해주세요.")
        val schedule = DepartureSchedule.from(ZonedDateTime.now(), s.departureMinuteOfDay)
        scheduleTicker?.cancel()
        mutable.update { it.copy(stage = JourneyStage.BUDGET, scheduleNow = schedule.now,
            departureAt = schedule.departure, plan = it.plan.copy(hours = schedule.hours)) }
    }

    fun backToSchedule() {
        if (mutable.value.stage != JourneyStage.BUDGET) return
        invalidateRoute()
        mutable.update { it.copy(stage = JourneyStage.SCHEDULE, scheduleNow = ZonedDateTime.now()) }
        startScheduleTicker()
    }

    private fun startScheduleTicker() {
        scheduleTicker?.cancel()
        if (!foreground || mutable.value.stage != JourneyStage.SCHEDULE) return
        scheduleTicker = viewModelScope.launch {
            while (isActive) {
                mutable.update { it.copy(scheduleNow = ZonedDateTime.now()) }
                delay(1_000)
            }
        }
    }

    fun backToRecommendations() {
        if (mutable.value.stage in listOf(JourneyStage.BUDGET, JourneyStage.SCHEDULE, JourneyStage.HOME_SETUP)) {
            invalidateRoute()
            scheduleTicker?.cancel()
            homeGeneration++
            homeJob?.cancel(); registrationJob?.cancel(); registrationPending = false
            mutable.update { it.copy(stage = JourneyStage.RECOMMEND, homeLoading = false) }
        }
    }

    fun depart() {
        val s = mutable.value
        if (s.stage != JourneyStage.BUDGET || s.busy) return
        if (s.confirmedHome == null) return message("귀가할 집을 먼저 확인해주세요.")
        val b = s.journeyBudget ?: return message("차량 배터리를 먼저 확인하세요")
        if (!b.feasible) return message("한 번의 충전으로도 부족합니다 · 추가 충전 계획이 필요해요")
        val via = b.chargeKwh > 0.001
        if (via && !b.chargerReachable) return message("목업 충전소까지 도달할 전력이 부족합니다")
        mutable.update { it.copy(viaCharger = via, chargeTarget = ceil(b.targetSoc)) }
        requestRoute(s.site.point, if (via) MockTrip.charger else null,
            if (via) JourneyStage.TO_CHARGER else JourneyStage.TO_CAMP)
    }

    private fun invalidateRoute() {
        NavigationSessionService.stop(getApplication())
        routeGeneration++
        navigationMonitor?.cancel(); navigationMonitor = null
        route?.cancel()
        route = null
        mutable.update { it.copy(busy = false, pendingRoute = null, routeMessage = "", routeError = false,
            navigationArrived = false, navigationEnded = false, navigationProblem = null) }
    }

    private fun requestRoute(destination: TripPoint, waypoint: TripPoint?, next: JourneyStage) {
        if (mutable.value.busy) return
        try { NavigationSessionService.start(getApplication()) }
        catch (_: IllegalStateException) { return message("이동 알림 연결을 시작하지 못했어요. 앱에서 다시 출발해주세요.") }
        val token = ++routeGeneration
        val previousMonitor = navigationMonitor
        previousMonitor?.cancel()
        val ready = CompletableDeferred<Unit>()
        mutable.update { it.copy(busy = true, pendingRoute = null, routeError = false,
            navigationArrived = false, navigationEnded = false, navigationProblem = null, routeMessage = "Pleos 내비게이션 경로 요청 중…") }
        navigationMonitor = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            previousMonitor?.join()
            try {
                navigator.navigationEvents(destination, waypoint).collect { event ->
                    if (routeGeneration != token) return@collect
                    when (event) {
                        NavigationEvent.READY -> ready.complete(Unit)
                        NavigationEvent.DESTINATION_ARRIVED -> {
                            if (next != JourneyStage.TO_CHARGER) mutable.update {
                                it.copy(navigationArrived = true, navigationProblem = null)
                            }
                        }
                        NavigationEvent.WAYPOINT_ARRIVED -> {
                            if (next == JourneyStage.TO_CHARGER) mutable.update {
                                it.copy(navigationArrived = true, navigationProblem = null)
                            }
                        }
                        NavigationEvent.CANCELLED -> {
                            if (!mutable.value.busy && !mutable.value.navigationArrived)
                                mutable.update { it.copy(navigationEnded = true,
                                    navigationProblem = if(next == JourneyStage.TO_CAMP)
                                        "주차한 곳에서 차박을 시작하거나 다시 안내를 받을 수 있어요."
                                    else "안내를 종료했어요. 필요하면 다시 안내를 받아보세요.") }
                        }
                    }
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                ready.completeExceptionally(e)
                if (routeGeneration == token) mutable.update {
                    it.copy(navigationProblem = "도착 알림 연결을 확인해주세요.")
                }
            }
        }
        route = viewModelScope.launch {
            val result = try {
                withTimeout(10_000) { ready.await() }
                navigator.requestRoute(destination, waypoint)
            } catch (_: TimeoutCancellationException) {
                Result.failure(IllegalStateException("도착 알림 연결 시간이 초과됐어요."))
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { Result.failure(e) }
            if (routeGeneration != token) return@launch
            if (result.isFailure) {
                navigationMonitor?.cancel()
                NavigationSessionService.stop(getApplication())
            }
            mutable.update { it.copy(busy = false, routeError = result.isFailure,
                stage = if (result.getOrNull()?.confirmed == true) next else it.stage,
                pendingRoute = if (result.getOrNull()?.confirmed == false) next else null,
                routeMessage = when {
                    result.isFailure -> result.exceptionOrNull()?.message ?: "경로 요청 실패"
                    result.getOrNull()?.confirmed == true -> "Pleos 목적지·경유지·안내 상태 확인"
                    else -> "SDK 요청 전송 · 경로 상태 응답 미확인. 내비게이션에서 목적지와 경유지를 확인한 후 진행하세요."
                }) }
        }
    }

    suspend fun showNavigation() {
        val result = navigator.showNavigation()
        if (result.isFailure) mutable.update { it.copy(navigationProblem = "내비 화면을 열지 못했어요. 다시 연결해주세요.") }
    }

    fun retryNavigation() {
        val s = mutable.value
        if (s.busy || s.stage !in listOf(JourneyStage.TO_CHARGER, JourneyStage.TO_CAMP, JourneyStage.HOME)) return
        val destination = if (s.returning) s.confirmedHome?.point ?: return else s.site.point
        requestRoute(destination, if (s.stage == JourneyStage.TO_CHARGER) MockTrip.charger else null, s.stage)
    }

    fun confirmRoute() {
        val next = mutable.value.pendingRoute ?: return
        mutable.update { it.copy(stage = next, pendingRoute = null,
            routeMessage = "사용자가 내비게이션 경로를 확인했습니다 · SDK 자동 확인 아님") }
    }

    fun arriveCharger() {
        val s = mutable.value
        if (s.stage != JourneyStage.TO_CHARGER || s.busy) return
        if (!s.navigationArrived || !s.fresh || s.sdk.parked != true) return message("충전소 도착 알림과 최신 P단 정보를 확인해야 합니다")
        navigationMonitor?.cancel()
        val energy = s.energy
        val required = if (s.returning) (s.chargerToHomeKm ?: return) / s.plan.efficiency + s.plan.capacity * s.plan.reserve / 100
            else JourneyBudget.calculate(s.plan, s.selected.distance - MockTrip.chargerKm,
                (s.homeKm ?: return), energy).required
        mutable.update { it.copy(stage = JourneyStage.CHARGING, energy = energy,
            chargeTarget = ceil(required / s.plan.capacity * 100), message = "목표 잔량을 확인하세요 · 차량 충전 시작/종료는 제어하지 않습니다") }
    }

    fun continueAfterCharge() {
        val s = mutable.value
        if (s.stage != JourneyStage.CHARGING || s.busy) return
        if (!s.fresh || (s.observedSoc ?: 0.0) + .001 < s.chargeTarget) return message("목표 잔량까지 충전한 뒤 다시 확인하세요")
        mutable.update { it.copy(charged = true) }
        val destination = if (s.returning) s.confirmedHome?.point ?: return else s.site.point
        requestRoute(destination, null,
            if (s.returning) JourneyStage.HOME else JourneyStage.TO_CAMP)
    }

    fun startStay() {
        val s = mutable.value
        if (s.phase == TripPhase.STAY) return
        if (!s.canStartStay)
            return message("도착 또는 안내 종료 후 P단과 최신 차량 정보를 확인하세요")
        navigationMonitor?.cancel()
        NavigationSessionService.stop(getApplication())
        mutable.update { it.copy(stage = JourneyStage.STAY, phase = TripPhase.STAY,
            navigationProblem = null, running = true, message = "차박을 시작했어요 · 차량 기본 캠핑 모드는 차량에서 설정하세요",
            history = listOf((it.energy / it.plan.capacity * 100).toFloat())) }
        startClock()
    }

    fun goHome() {
        val s = mutable.value
        if (s.stage != JourneyStage.STAY || s.busy || !s.fresh) return
        val home = s.confirmedHome ?: return message("귀가할 집을 먼저 확인해주세요.")
        val required = (s.homeKm ?: return) / s.plan.efficiency + s.plan.capacity * s.plan.reserve / 100
        val via = s.energy < required
        val chargerDistance = s.returnChargerKm
        if (via && s.energy < chargerDistance / s.plan.efficiency) return message("경유 충전소까지 부족합니다 · 이동 대신 충전 지원이 필요합니다")
        stop()
        clock?.cancel()
        mutable.update { it.copy(returning = true, viaCharger = via, charged = false, running = false) }
        requestRoute(home.point, if (via) MockTrip.charger else null,
            if (via) JourneyStage.TO_CHARGER else JourneyStage.HOME)
    }

    fun arriveHome() {
        val s = mutable.value
        if (s.stage != JourneyStage.HOME) return
        if (!s.navigationArrived || !s.fresh || s.sdk.parked != true) return message("귀가 도착 알림과 최신 P단 정보를 확인하세요")
        navigationMonitor?.cancel()
        mutable.update { it.copy(stage = JourneyStage.COMPLETE, running = false,
            message = "이동과 휴식을 한 여정으로 마쳤습니다") }
    }

    private fun startClock() {
        clock?.cancel()
        clock = viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                advance()
            }
        }
    }
    private fun advance() {
        val s = mutable.value
        if (s.phase != TripPhase.STAY || s.stage != JourneyStage.STAY || s.remaining <= 0) return
        val minutes = minOf(1.0, s.remaining * 60)
        mutable.update { it.copy(elapsed = it.elapsed + minutes / 60,
            history = (it.history + (it.energy / it.plan.capacity * 100).toFloat()).takeLast(80)) }
        if (mutable.value.remaining <= 0) { stop("계획한 체류가 끝났습니다 · 귀가를 시작할 수 있어요"); clock?.cancel() }
    }

    fun manualTemperature(delta: Double) {
        val s = mutable.value
        if (!s.canSetTemperature) return message("주차·온도·제어 지원을 먼저 확인하세요")
        stop("수동 설정 변경")
        val current = s.sdk.temperature ?: return
        applyTemperature((current + delta).coerceIn(s.plan.minTemp, s.plan.maxTemp))
    }
    private fun applyTemperature(target: Double) {
        val s = mutable.value
        if (s.applying) return
        val token = generation
        mutable.update { it.copy(applying = true, message = "설정 온도 적용 확인 중…") }
        command = viewModelScope.launch {
            val result = vehicle?.requestTemperature(target) ?: Result.failure(IllegalStateException("SDK 연결 없음"))
            if (token != generation) return@launch
            result.fold(onSuccess = { actual ->
                mutable.update { it.copy(temperature = actual, applying = false,
                    sdk = it.sdk.copy(temperature = actual),
                    message = "SDK 온도 적용 및 재조회 확인 · 절감량은 미측정") }
            }, onFailure = {
                stop("공조 적용 실패 · " + (it.message ?: "상태 확인 필요"))
                mutable.update { state -> state.copy(fault = "명령 실패") }
            })
        }
    }
    fun stop(reason: String = "자동 제어 중지") {
        generation++
        command?.cancel()
        command = null
        mutable.update { it.copy(applying = false, message = reason) }
    }

    fun reset() {
        homeGeneration++
        homeJob?.cancel(); registrationJob?.cancel(); registrationPending = false
        invalidateRoute()
        clock?.cancel(); scheduleTicker?.cancel(); stop()
        val s = mutable.value
        mutable.value = CampilotUiState(plan = s.plan, sdk = s.sdk, loaded = s.loaded,
            energy = s.energy, temperature = s.sdk.temperature ?: s.plan.temperature)
    }
    fun refreshVehicle() {
        poll?.cancel(); startPolling()
    }
    private fun startPolling() {
        if (poll?.isActive == true) return
        val gateway = vehicle ?: PleosVehicleGateway(getApplication()).also { vehicle = it }
        poll = viewModelScope.launch {
            while (isActive) {
                try {
                    val snapshot = gateway.refresh()
                    mutable.update { state ->
                        state.copy(sdk = snapshot, energy = (snapshot.soc ?: 0.0) * state.plan.capacity / 100,
                            temperature = snapshot.temperature ?: state.temperature)
                    }
                } catch (e: CancellationException) { throw e
                } catch (e: Exception) {
                    mutable.update { it.copy(sdk = PleosSnapshot(message = e.message ?: "SDK 연결 실패")) }
                }
                delay(5_000)
            }
        }
    }
    fun background() {
        foreground = false
        clock?.cancel(); poll?.cancel(); scheduleTicker?.cancel(); stop("화면 비활성화 · 자동 공조 중단")
        // Native navigation opens another Activity. Keep that explicit route request alive.
        mutable.update { it.copy(running = false) }
    }
    fun resume() {
        foreground = true
        if (mutable.value.navigationArrived || mutable.value.navigationEnded) NavigationSessionService.stop(getApplication())
        if (mutable.value.stage == JourneyStage.HOME_SETUP && (registrationPending || homeJob?.isActive != true)) {
            registrationPending = false
            refreshHome()
        }
        startScheduleTicker()
        startPolling()
        if (mutable.value.stage == JourneyStage.STAY && mutable.value.remaining > 0) startClock()
    }
    private fun message(text: String) { mutable.update { it.copy(message = text) } }
    override fun onCleared() {
        NavigationSessionService.stop(getApplication())
        clock?.cancel(); command?.cancel(); route?.cancel(); navigationMonitor?.cancel(); poll?.cancel(); scheduleTicker?.cancel()
        homeJob?.cancel(); registrationJob?.cancel()
        vehicle?.close()
    }
}
