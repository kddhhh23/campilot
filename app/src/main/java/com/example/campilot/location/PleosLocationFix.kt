package com.example.campilot.location

import java.util.Locale

data class PleosLocationFix(val latitude: Double, val longitude: Double, val address: String) {
    val coordinates: String get() = String.format(Locale.KOREA, "위도 %.5f · 경도 %.5f", latitude, longitude)
    companion object {
        fun from(latitude: Double, longitude: Double, address: String): PleosLocationFix? {
            if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 ||
                longitude !in -180.0..180.0) return null
            // Some navigation providers return an empty (0,0) sentinel while acquiring position.
            if (latitude == 0.0 && longitude == 0.0 && address.isBlank()) return null
            return PleosLocationFix(latitude, longitude, address.trim())
        }
    }
}
