package com.example.campilot

import com.example.campilot.domain.TripPoint
import com.example.campilot.navigation.NavigationArrivalGate
import org.junit.Assert.*
import org.junit.Test

class NavigationArrivalTest {
    private val target = TripPoint("솔숲 캠핑장 (목업)", 37.7, 127.2)
    @Test fun arrivalRequiresTheRequestedDestination() {
        val gate = NavigationArrivalGate(target)
        assertFalse(gate.accepts(target.name))
        gate.updateDestination(37.7, 127.2)
        assertTrue(gate.accepts(target.name))
        assertTrue(gate.accepts(""))
        assertFalse(gate.accepts("다른 캠핑장"))
    }
    @Test fun changedOrInvalidDestinationCannotArrive() {
        val gate = NavigationArrivalGate(target)
        gate.updateDestination(37.7, 127.2)
        gate.updateDestination(37.0, 127.0)
        assertFalse(gate.accepts(target.name))
        gate.updateDestination(Double.NaN, 127.2)
        assertFalse(gate.accepts(target.name))
    }
    @Test fun cancellationInvalidatesPreviouslyMatchedDestination() {
        val gate = NavigationArrivalGate(target)
        gate.updateDestination(37.7, 127.2)
        gate.reset()
        assertFalse(gate.accepts(target.name))
    }
}
