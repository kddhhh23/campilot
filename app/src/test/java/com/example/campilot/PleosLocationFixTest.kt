package com.example.campilot

import com.example.campilot.location.PleosLocationFix
import org.junit.Assert.*
import org.junit.Test

class PleosLocationFixTest {
    @Test fun unavailableSentinelDoesNotBecomeCurrentLocation() {
        assertNull(PleosLocationFix.from(0.0, 0.0, ""))
        assertNull(PleosLocationFix.from(Double.NaN, 127.0, "invalid"))
        assertNull(PleosLocationFix.from(91.0, 127.0, "invalid"))
    }
    @Test fun validSdkLocationRetainsCoordinatesAndAddress() {
        val fix = PleosLocationFix.from(37.4979, 127.0276, "  강남역  ")!!
        assertEquals("위도 37.49790 · 경도 127.02760", fix.coordinates)
        assertEquals("강남역", fix.address)
    }
    @Test fun validWesternHemisphereIsNotTreatedAsSentinel() {
        assertNotNull(PleosLocationFix.from(40.7, -74.0, ""))
    }
}
