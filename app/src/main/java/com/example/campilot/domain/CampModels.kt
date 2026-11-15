package com.example.campilot.domain
data class CampPlan(val capacity:Double=60.0,val soc:Double=60.0,val efficiency:Double=5.0,val hours:Double=8.0,val nextKm:Double=25.0,val basePower:Double=0.2,val climatePower:Double=1.0,val totalPower:Double?=null,val temperature:Double=22.0,val minTemp:Double=18.0,val maxTemp:Double=26.0,val cooling:Boolean=false) {
 val reserve:Double get()=10.0
}
enum class TripPhase { BEFORE_TRAVEL, STAY }
sealed interface Budget {
 data class Invalid(val reason:String):Budget
 data class Valid(val travel:Double,val stay:Double,val onward:Double,val reserve:Double,val arrival:Double,val endLow:Double,val endHigh:Double,val extra:Double,val targetSoc:Double,val feasible:Boolean,val reachable:Boolean):Budget
}
data class Campsite(val id:String,val name:String,val distance:Double,val landscape:String,val facilities:String)
val demoCampsites=listOf(Campsite("pine","솔숲 캠핑장",20.0,"소나무 그늘 아래 조용한 밤","차량 숙박 허용 · 화장실 · 개수대"),Campsite("lake","호수 캠핑장",50.0,"물가를 따라 펼쳐지는 저녁","차량 숙박 허용 · 샤워실 · 산책로"),Campsite("hill","별빛 캠핑장",90.0,"도심에서 멀리, 별과 가까이","차량 숙박 허용 · 화장실 · 전망대"))
