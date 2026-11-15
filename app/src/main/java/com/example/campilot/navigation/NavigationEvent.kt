package com.example.campilot.navigation

import com.example.campilot.domain.TripPoint
import kotlin.math.abs

enum class NavigationEvent { READY, DESTINATION_ARRIVED, WAYPOINT_ARRIVED, CANCELLED }

/** Arrival has no coordinates: accept it only after the SDK destination matches our route. */
class NavigationArrivalGate(private val expected: TripPoint) {
    @Volatile private var matched = false
    fun updateDestination(latitude: Double, longitude: Double) {
        matched = latitude.isFinite() && longitude.isFinite() &&
            abs(latitude - expected.latitude) < .001 && abs(longitude - expected.longitude) < .001
    }
    fun reset() { matched = false }
    fun accepts(name: String): Boolean =
        matched && (name.isBlank() || name.trim() == expected.name.trim())
}
