package com.example.campilot.domain

import kotlin.math.max

enum class JourneyStage { RECOMMEND, HOME_SETUP, SCHEDULE, BUDGET, TO_CHARGER, CHARGING, TO_CAMP, STAY, HOME, COMPLETE }
data class TripPoint(val name: String, val latitude: Double, val longitude: Double)
data class TripSite(val campsite: Campsite, val point: TripPoint)

object MockTrip {
    val origin = TripPoint("서울시청 인근", 37.5665, 126.9780)
    val charger = TripPoint("목업 경유 충전소", 37.5890, 127.0350)
    const val chargerKm = 5.0
    val sites = demoCampsites.mapIndexed { index, site ->
        TripSite(site, TripPoint(site.name + " (목업)", 37.70 + index * .03, 127.20 + index * .05))
    }
}

/** Distances and consumption are planning assumptions, never measured vehicle telemetry. */
data class JourneyBudget(
    val required: Double, val reserveBattery: Double,
    val chargeKwh: Double, val targetSoc: Double,
    val feasible: Boolean, val chargerReachable: Boolean
) {
    companion object {
        fun calculate(p: CampPlan, distance: Double, homeKm: Double,
                      energy: Double = p.capacity * p.soc / 100): JourneyBudget {
            require(EnergyCalculator.calculate(p, distance, energy = energy) is Budget.Valid)
            require(homeKm.isFinite() && homeKm >= 0)
            val reserve = p.capacity * p.reserve / 100
            val required = distance / p.efficiency + p.hours * (p.totalPower ?: p.basePower + p.climatePower) + homeKm / p.efficiency + reserve
            return JourneyBudget(required, reserve, max(0.0, required - energy),
                required / p.capacity * 100, required <= p.capacity,
                energy >= MockTrip.chargerKm / p.efficiency)
        }
    }
}
object ControlGuard {
    fun canControl(parked: Boolean?, temperature: Double?, writable: Boolean,
                   sampledAt: Long, now: Long): Boolean =
        parked == true && temperature != null && temperature.isFinite() && writable &&
            sampledAt > 0 && now >= sampledAt && now - sampledAt <= 10_000
}
