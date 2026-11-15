package com.example.campilot

import com.example.campilot.domain.*
import org.junit.Assert.*
import org.junit.Test

class JourneyTest {
    private val plan = CampPlan(soc = 35.0, nextKm = 25.0)
    @Test fun shortReturnUsesFixedTenPercentReserve() {
        val b = JourneyBudget.calculate(plan, 20.0, 25.0)
        assertEquals(6.0, b.reserveBattery, 0.0001)
        assertEquals(24.6, b.required, 0.0001)
        assertEquals(3.6, b.chargeKwh, 0.0001)
    }
    @Test fun longReturnUsesReturnEnergyPlusReserve() {
        val b = JourneyBudget.calculate(plan, 20.0, 100.0)
        assertEquals(6.0, b.reserveBattery, 0.0001)
        assertEquals(39.6, b.required, 0.0001)
    }
    @Test fun chargingAtWaypointExcludesAlreadyDrivenDistance() {
        val b = JourneyBudget.calculate(plan, 15.0, 25.0, energy = 20.0)
        assertEquals(23.6, b.required, 0.0001)
        assertEquals(23.6 / 60.0 * 100, b.targetSoc, 0.0001)
    }
    @Test fun impossiblePlanDoesNotClampTargetSoc() {
        val b = JourneyBudget.calculate(plan.copy(hours = 100.0),20.0,25.0)
        assertFalse(b.feasible); assertTrue(b.targetSoc > 100)
    }
    @Test fun unreachableChargerBlocksDeparture() {
        assertFalse(JourneyBudget.calculate(plan,20.0,25.0,energy = 0.5).chargerReachable)
    }
    @Test fun invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            JourneyBudget.calculate(plan.copy(hours = Double.NaN),20.0,25.0)
        }
    }
    @Test fun unknownOrStaleVehicleCannotControlClimate() {
        assertFalse(ControlGuard.canControl(null, 22.0, true, 1000, 1000))
        assertFalse(ControlGuard.canControl(true, 22.0, true, 1000, 20000))
        assertFalse(ControlGuard.canControl(false, 22.0, true, 1000, 1000))
        assertFalse(ControlGuard.canControl(true, null, true, 1000, 1000))
        assertFalse(ControlGuard.canControl(true, 22.0, false, 1000, 1000))
        assertTrue(ControlGuard.canControl(true, 22.0, true, 1000, 1000))
    }
}
