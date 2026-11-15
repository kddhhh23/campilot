package com.example.campilot.domain

import kotlin.math.*

data class HomeDestination(val point: TripPoint, val address: String) {
    val label: String get() = address.ifBlank { point.name }
}

/** Provisional distance, not a navigation road route. Always disclose the 1.3 factor. */
object HomeDistanceEstimate {
    fun km(from: TripPoint, to: TripPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 6371.0 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0))) * 1.3
    }
}
