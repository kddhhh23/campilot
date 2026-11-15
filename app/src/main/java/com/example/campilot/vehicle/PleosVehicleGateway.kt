package com.example.campilot.vehicle

import android.content.Context
import android.os.SystemClock
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import com.example.campilot.domain.ControlGuard
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

data class PleosSnapshot(
    val soc: Double? = null,
    val temperature: Double? = null,
    val parked: Boolean? = null,
    val writable: Boolean = false,
    val sampledAt: Long = 0,
    val message: String = "Pleos 차량 연결 전",
    val climateMessage: String = "공조 지원 확인 전"
)

/** UI owner starts polling; this adapter never owns a detached coroutine. */
class PleosVehicleGateway(context: Context) {
    private val vehicle = Vehicle(context.applicationContext)
    private val lock = Mutex()
    private var started = false
    private var closed = false
    private var area: SeatArea? = null
    private var minimum = 16f
    private var maximum = 30f
    private var step = .5f
    private val mutable = MutableStateFlow(PleosSnapshot())
    val snapshots = mutable.asStateFlow()

    suspend fun refresh(): PleosSnapshot = lock.withLock { refreshLocked() }

    private suspend fun refreshLocked(): PleosSnapshot {
        check(!closed) { "SDK 연결이 해제되었습니다" }
        if (!started) { vehicle.initialize(); started = true; delay(500) }
        // Use the oldest possible sample time; slow later queries must not renew old values.
        val sampledAt = SystemClock.elapsedRealtime()
        var issue = "SDK 조회 완료"
        var climateIssue = "공조 조회 불가"
        val battery = try {
            val capability = awaitSdk<AccessType> { ok, fail ->
                vehicle.getEvBattery().checkEvBatteryStateOfChargeCapabilities({ ok(it.accessType) }, fail)
            }
            check(capability == AccessType.READ || capability == AccessType.READ_WRITE) { "배터리 조회 미지원" }
            awaitSdk<Int?> { ok, fail -> vehicle.getEvBattery().getEvBatteryStateOfCharge(ok, fail) }
                ?.takeIf { it in 0..100 }?.toDouble()
                ?: error("배터리 값 없음")
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { issue = "배터리 조회 실패: " + e.shortMessage(); null }
        var writable = false
        val temp = try {
            val capability = awaitSdk<AccessType> { ok, fail ->
                vehicle.getHVAC().checkHvacTemperatureCapabilities({ config, areas, temperatures ->
                    // Never choose an arbitrary passenger zone.
                    val entry = areas.entries.firstOrNull { SeatValue.ROW_1_LEFT in it.key.values }
                    area = entry?.key
                    minimum = temperatures?.minCelsius ?: entry?.value?.minValue ?: 16f
                    maximum = temperatures?.maxCelsius ?: entry?.value?.maxValue ?: 30f
                    step = temperatures?.incrementCelsius ?: .5f
                    ok(config.accessType)
                }, fail)
            }
            writable = capability == AccessType.READ_WRITE
            check(capability == AccessType.READ || capability == AccessType.READ_WRITE) { "공조 조회 미지원" }
            val selected = checkNotNull(area) { "운전석 공조 영역 미지원" }
            val value = awaitSdk<Float?> { ok, fail ->
                vehicle.getHVAC().getHvacTemperature(selected, TemperatureUnit.CELSIUS, ok, fail)
            }?.toDouble()?.takeIf { it.isFinite() } ?: error("공조 값 없음")
            climateIssue = if (writable) "운전석 설정 온도 조회·제어 지원" else "운전석 설정 온도 읽기 전용"
            value
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { writable = false; climateIssue = e.shortMessage(); null }
        @Suppress("DEPRECATION")
        val parked = try {
            awaitSdk<VehicleGear?> { ok, fail ->
                vehicle.getGear().getCurrentGearSelectionState(ok, fail)
            }?.let { it == VehicleGear.GEAR_PARK }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { null }
        val value = PleosSnapshot(battery, temp, parked, writable, sampledAt, issue, climateIssue)
        if (!closed) mutable.value = value
        return value
    }

    suspend fun requestTemperature(target: Double): Result<Double> = lock.withLock {
        try {
            val fresh = refreshLocked()
            check(ControlGuard.canControl(fresh.parked, fresh.temperature, fresh.writable,
                fresh.sampledAt, SystemClock.elapsedRealtime())) { "주차·공조 권한·최신 상태 확인 필요" }
            check(target.isFinite() && target >= minimum && target <= maximum) { "차량 허용 온도 범위 밖" }
            check(step.isFinite() && step > 0 && abs((target - minimum) / step - kotlin.math.round((target - minimum) / step)) < .01) {
                "차량 온도 조절 단위와 맞지 않습니다"
            }
            val zone = checkNotNull(area)
            awaitSdk<Float?> { ok, fail ->
                vehicle.getHVAC().setHvacTemperature(zone, target.toFloat(), TemperatureUnit.CELSIUS,
                    { _, value -> ok(value) }, fail)
            }
            delay(300)
            val actual = awaitSdk<Float?> { ok, fail ->
                vehicle.getHVAC().getHvacTemperature(zone, TemperatureUnit.CELSIUS, ok, fail)
            }?.toDouble()
            check(actual != null && abs(actual - target) < .1) { "설정 온도 재조회 불일치" }
            mutable.value = fresh.copy(temperature = actual)
            Result.success(actual)
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { Result.failure(e) }
    }

    fun close() {
        closed = true
        if (started) vehicle.release()
        mutable.value = PleosSnapshot(message = "Pleos 연결 해제")
    }
}
internal suspend fun <T> awaitSdk(block: ((T) -> Unit, (Exception) -> Unit) -> Unit): T =
    try {
        withTimeout(5_000) {
            suspendCancellableCoroutine { continuation ->
                block({ value -> if (continuation.isActive) continuation.resume(value) },
                    { error -> if (continuation.isActive) continuation.resumeWithException(error) })
            }
        }
    } catch (_: TimeoutCancellationException) {
        throw IllegalStateException("차량 SDK 응답 시간 초과")
    }
internal fun Throwable.shortMessage(): String {
    val detail = message ?: ""
    return when {
        this is SecurityException || detail.contains("permission", ignoreCase = true) -> "차량 정보 접근 권한이 필요합니다"
        detail.contains("PropertyNotAvailable") -> "현재 차량 상태에서 공조 정보를 사용할 수 없습니다"
        detail.contains("timeout", ignoreCase = true) || detail.contains("시간 초과") -> "차량 응답이 지연됩니다. 다시 확인해주세요"
        detail.any { it in '가'..'힣' } -> detail.take(100)
        else -> "차량 연결 또는 기능 지원 상태를 확인해주세요"
    }
}
