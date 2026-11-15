package com.example.campilot
import com.example.campilot.domain.*
import org.junit.Assert.*
import org.junit.Test
class EnergyTest {
 @Test fun fixedExample() { val b=EnergyCalculator.calculate(CampPlan(soc=50.0),50.0) as Budget.Valid
 assertEquals(0.6,b.extra,1e-6); assertEquals(51.0,b.targetSoc,1e-6); assertEquals(10.4,b.endLow,1e-6) }
 @Test fun stayDoesNotChargeTravelTwice() { val p=CampPlan(soc=50.0); val b=EnergyCalculator.calculate(p,50.0,TripPhase.STAY,20.0) as Budget.Valid; assertEquals(10.4,b.endLow,1e-6) }
 @Test fun invalidNumbers() { for (v in listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0,101.0)) assertTrue(EnergyCalculator.calculate(CampPlan(soc=v),0.0) is Budget.Invalid) }
 @Test fun missingCapacity() { assertTrue(EnergyCalculator.calculate(CampPlan(capacity=0.0),0.0) is Budget.Invalid) }
 @Test fun longerStayUsesMore() { val a=EnergyCalculator.calculate(CampPlan(hours=4.0),20.0) as Budget.Valid; val b=EnergyCalculator.calculate(CampPlan(hours=8.0),20.0) as Budget.Valid; assertTrue(a.endLow>b.endLow) }
 @Test fun impossibleNotClamped() { val b=EnergyCalculator.calculate(CampPlan(hours=80.0),50.0) as Budget.Valid; assertFalse(b.feasible); assertTrue(b.targetSoc>100) }
 @Test fun totalPowerNotDoubleCounted() { val a=EnergyCalculator.calculate(CampPlan(),50.0) as Budget.Valid; val b=EnergyCalculator.calculate(CampPlan(totalPower=1.2),50.0) as Budget.Valid; assertEquals(a.endLow,b.endLow,1e-6) }
 @Test fun exactReserveBoundary() { val b=EnergyCalculator.calculate(CampPlan(soc=51.0),50.0) as Budget.Valid; assertEquals(0.0,b.extra,1e-6) }
 @Test fun rankByBudgetThenDistance() { val p=CampPlan(soc=45.0); val ranked=rankCampsites(p); assertEquals("pine",ranked.first().id) }
}