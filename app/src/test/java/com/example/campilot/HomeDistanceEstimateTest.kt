package com.example.campilot

import com.example.campilot.domain.*
import org.junit.Assert.*
import org.junit.Test

class HomeDistanceEstimateTest {
    @Test fun sameLocationDoesNotInventReturnDistance() {
        val p = TripPoint("same", 37.5, 127.0)
        assertEquals(0.0, HomeDistanceEstimate.km(p,p), 1e-8)
    }
    @Test fun equatorialDegreeAppliesExplicitEstimateFactor() {
        val km = HomeDistanceEstimate.km(TripPoint("camp",0.0,0.0),TripPoint("home",0.0,1.0))
        assertEquals(144.5534, km, .001)
    }
    @Test fun fartherHomeChangesChargeDecision() {
        val camp = TripPoint("camp",0.0,0.0)
        val close = HomeDistanceEstimate.km(camp,TripPoint("close",0.0,.1))
        val far = HomeDistanceEstimate.km(camp,TripPoint("far",0.0,1.0))
        val p = CampPlan(soc=60.0,hours=8.0)
        assertEquals(0.0, JourneyBudget.calculate(p,20.0,close).chargeKwh, 1e-6)
        assertTrue(JourneyBudget.calculate(p,20.0,far).chargeKwh > 0)
    }
}
