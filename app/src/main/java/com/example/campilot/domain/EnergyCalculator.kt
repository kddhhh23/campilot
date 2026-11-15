package com.example.campilot.domain
import kotlin.math.max
object EnergyCalculator {
 fun calculate(p:CampPlan,distance:Double,phase:TripPhase=TripPhase.BEFORE_TRAVEL,energy:Double=p.capacity*p.soc/100,hours:Double=p.hours,power:Double=p.totalPower?:p.basePower+p.climatePower):Budget {
 val nums=listOf(p.capacity,p.soc,p.efficiency,p.hours,p.nextKm,p.reserve,p.basePower,p.climatePower,p.temperature,p.minTemp,p.maxTemp,distance,energy,hours,power)
 if(nums.any{!it.isFinite()} || p.capacity<=0 || p.efficiency<=0 || p.soc !in 0.0..100.0 || p.reserve !in 0.0..100.0 || listOf(p.hours,p.nextKm,p.basePower,p.climatePower,distance,energy,hours,power).any{it<0} || p.minTemp>p.maxTemp || p.temperature !in p.minTemp..p.maxTemp) return Budget.Invalid("입력 범위와 차량 정보를 확인하세요")
 val travel=if(phase==TripPhase.BEFORE_TRAVEL) distance/p.efficiency else 0.0
 val stay=hours*power; val onward=p.nextKm/p.efficiency; val reserve=p.capacity*p.reserve/100
 val required=travel+stay+onward+reserve
 return Budget.Valid(travel,stay,onward,reserve,energy-travel,energy-travel-stay,energy-travel-stay*0.8,max(0.0,required-energy),required/p.capacity*100,required<=p.capacity,energy>=travel)
 }
}
fun rankCampsites(plan:CampPlan)=demoCampsites.sortedWith(compareBy<Campsite>{ val b=EnergyCalculator.calculate(plan,it.distance); if(b is Budget.Valid && b.extra<1e-6) 0 else 1 }.thenBy{it.distance}.thenBy{it.id})
