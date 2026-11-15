package com.example.campilot.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campilot.f
import com.example.campilot.domain.*

data class JourneyActions(
    val recommend:()->Unit={},val select:(Campsite)->Unit={},val depart:()->Unit={},
    val arriveCharger:()->Unit={},val continueAfterCharge:()->Unit={},
    val startStay:()->Unit={},val home:()->Unit={},val arriveHome:()->Unit={},
    val stop:()->Unit={},val temperature:(Double)->Unit={},
    val reset:()->Unit={},
    val refresh:()->Unit={},val settings:()->Unit={},
    val back:()->Unit={},val confirmRoute:()->Unit={},val retryNavigation:()->Unit={},
    val refreshHome:()->Unit={},val confirmHome:()->Unit={},val registerHome:()->Unit={},val editHome:()->Unit={},
    val chooseTime:(Int)->Unit={},val confirmSchedule:()->Unit={},val editSchedule:()->Unit={}
)

@Composable fun JourneyScreen(s:CampilotUiState,a:JourneyActions) {
    if (s.stage == JourneyStage.HOME_SETUP) {
        HomeScreen(s,a)
        return
    }
    if (s.stage == JourneyStage.SCHEDULE) {
        DepartureScreen(s,a)
        return
    }
    if (s.stage in listOf(JourneyStage.TO_CHARGER,JourneyStage.TO_CAMP,JourneyStage.HOME)) {
        TravelScreen(s,a)
        return
    }
    JourneyContent(s,a)
}

@Composable private fun JourneyContent(s:CampilotUiState,a:JourneyActions) {
    val stage=s.stage
    val budget=s.journeyBudget
    val pending=s.pendingRoute!=null
    var showChargeConsent by remember(stage,s.selected,s.plan,s.confirmedHome) { mutableStateOf(false) }
    val canDepartViaCharger = !s.busy && !pending && budget?.feasible == true &&
        budget.chargerReachable
    if(showChargeConsent && stage==JourneyStage.BUDGET) AlertDialog(
        onDismissRequest={showChargeConsent=false},
        title={Text("충전소를 경유할까요?")},
        text={Text("계획한 일정에 필요한 전력이 부족할 수 있어요. 체류 시간은 그대로 유지하고 가는 길에 충전소를 경유할까요?")},
        confirmButton={TextButton(enabled=canDepartViaCharger,onClick={
            showChargeConsent=false
            a.depart()
        }){Text("충전소 경유하기")}},
        dismissButton={TextButton(onClick={showChargeConsent=false}){Text("취소")}}
    )
    val charged=s.fresh && (s.observedSoc ?: 0.0)+.001>=s.chargeTarget && s.chargeTarget<=100
    val title=when(stage) {
        JourneyStage.RECOMMEND->if(s.recommendationsReady) "가까운 곳부터 골라보세요" else "차박을 준비해볼까요?"
        JourneyStage.HOME_SETUP->"귀가 목적지 확인"
        JourneyStage.SCHEDULE->"시간 정하기"
        JourneyStage.BUDGET->"떠나기 전에, 전력 확인"
        JourneyStage.TO_CHARGER->"충전소로 이동해요"
        JourneyStage.CHARGING->"이만큼 충전하면 준비 끝"
        JourneyStage.TO_CAMP->"이제 쉬러 갈까요?"
        JourneyStage.STAY->"편안한 밤을 보내세요"
        JourneyStage.HOME->"집으로 돌아가요"
        JourneyStage.COMPLETE->"여정을 마쳤어요"
    }
    val subtitle=when(stage) {
        JourneyStage.RECOMMEND->if(s.recommendationsReady) "차박지 한 곳을 선택하면 출발 시간을 정할 수 있어요." else "현재 위치를 확인한 뒤 차박지 추천을 받아보세요."
        JourneyStage.HOME_SETUP->"귀가할 집을 확인해요."
        JourneyStage.SCHEDULE->"내일 집으로 출발할 시간을 정해요."
        JourneyStage.BUDGET->s.selected.name+" · 현재부터 내일 출발까지 "+s.plan.hours.f()+"시간을 기준으로 계산해요."
        JourneyStage.TO_CHARGER->"충전을 마친 뒤 "+if(s.returning) "집으로 돌아갑니다." else "캠핑장으로 이동합니다."
        JourneyStage.CHARGING->"목표 잔량에 도달하면 다음 장소로 출발할 수 있어요."
        JourneyStage.TO_CAMP->"도착해서 안전하게 주차한 다음 차박을 시작하세요."
        JourneyStage.STAY->if(s.navigationArrived) s.selected.name+"에서 쉬는 중이에요." else "주차한 곳에서 쉬는 중이에요."
        JourneyStage.HOME->"주차를 마치면 아래 버튼으로 여정을 완료하세요."
        JourneyStage.COMPLETE->"이동부터 휴식, 귀가까지 함께했어요."
    }
    val actionLabel=when {
        s.busy->"경로 준비 중…"
        pending->"경로 확인했어요"
        stage==JourneyStage.RECOMMEND->"차박지 추천 받기"
        stage==JourneyStage.BUDGET && budget==null->"차량 정보 다시 확인"
        stage==JourneyStage.BUDGET && (budget?.feasible==false || (budget?.chargeKwh ?: 0.0)>0 && budget?.chargerReachable==false)->"충전 계획 확인 필요"
        stage==JourneyStage.BUDGET->if((budget?.chargeKwh ?: 0.0)>0.001) "출발하기" else "바로 출발"
        stage==JourneyStage.TO_CHARGER->"충전소 도착"
        stage==JourneyStage.CHARGING->if(s.returning) "집으로 출발" else "캠핑장으로 출발"
        stage==JourneyStage.TO_CAMP->"차박 시작"
        stage==JourneyStage.STAY->"귀가 시작"
        stage==JourneyStage.HOME->"귀가 완료"
        else->"새 여정 준비"
    }
    val actionEnabled=!s.busy && when {
        pending->true
        stage==JourneyStage.BUDGET && budget!=null->budget.feasible && (budget.chargeKwh<=0.001 || budget.chargerReachable)
        stage==JourneyStage.CHARGING->charged
        stage==JourneyStage.STAY->s.fresh
        stage in listOf(JourneyStage.TO_CHARGER,JourneyStage.TO_CAMP,JourneyStage.HOME)->s.fresh && s.sdk.parked==true
        else->true
    }
    val act:()->Unit={
        when {
            pending->a.confirmRoute()
            stage==JourneyStage.RECOMMEND->a.recommend()
            stage==JourneyStage.BUDGET && budget==null->a.refresh()
            stage==JourneyStage.BUDGET && (budget?.feasible==false || (budget?.chargeKwh ?: 0.0)>0 && budget?.chargerReachable==false)->Unit
            stage==JourneyStage.BUDGET->if((budget?.chargeKwh ?: 0.0)>0.001) showChargeConsent=true else a.depart()
            stage==JourneyStage.TO_CHARGER->a.arriveCharger()
            stage==JourneyStage.CHARGING->a.continueAfterCharge()
            stage==JourneyStage.TO_CAMP->a.startStay()
            stage==JourneyStage.STAY->a.home()
            stage==JourneyStage.HOME->a.arriveHome()
            else->a.reset()
        }
    }
    Scaffold(bottomBar={
        if(stage!=JourneyStage.RECOMMEND || !s.recommendationsReady) Surface(color=MaterialTheme.colorScheme.surface,shadowElevation=8.dp) {
            BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=24.dp,vertical=16.dp)) {
                val hint=when {
                    pending->"내비게이션의 목적지와 경유지를 확인한 뒤 눌러주세요."
                    stage==JourneyStage.CHARGING && !charged && s.chargeTarget>100->"한 번의 충전으로 부족해요. 여정을 다시 계획해주세요."
                    stage==JourneyStage.CHARGING && !charged->"목표 잔량까지 충전하면 출발 버튼이 활성화돼요."
                    stage==JourneyStage.BUDGET && !actionEnabled && !s.busy->"현재 충전 계획으로는 출발할 수 없어요. 전력 안내를 확인해주세요."
                    !actionEnabled && !s.busy->"최신 차량 정보와 주차 상태가 필요해요."
                    stage==JourneyStage.RECOMMEND->"현재 위치를 확인한 뒤 추천을 받아보세요."
                    else->"차량 잔량 사용 · 장소와 소비량은 시연용"
                }
                if(maxWidth>=750.dp && LocalDensity.current.fontScale<1.4f) Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                    Text(hint,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.width(360.dp)){PrimaryAction(actionLabel,actionEnabled,act)}
                } else Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(hint,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryAction(actionLabel,actionEnabled,act)
                }
            }
        }
    }) { padding ->
        key(stage) {
            BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
                val wide=maxWidth>=900.dp && LocalDensity.current.fontScale<1.4f
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if(wide) 32.dp else 20.dp),
                    verticalArrangement=Arrangement.spacedBy(24.dp)) {
                    if(stage==JourneyStage.RECOMMEND) Text("1  장소 고르기",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
                    else JourneyProgress(stage)
                    Text(title,style=MaterialTheme.typography.headlineLarge)
                    Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    if(s.routeError) Notice("경로를 준비하지 못했어요. 연결 상태를 확인하고 다시 시도해주세요.",true)
                    if(pending) Notice("내비게이션으로 요청을 보냈어요. 경로를 자동 확인하지 못했으니 목적지와 경유지를 직접 확인해주세요.")
                    if(s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    when(stage) {
                        JourneyStage.RECOMMEND->{
                            LiveLocationPanel()
                            if(s.recommendationsReady) {
                                if(wide) Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                                    MockTrip.sites.forEachIndexed { i,site ->
                                        SiteCard(site,i,{a.select(site.campsite)},Modifier.weight(1f))
                                    }
                                } else MockTrip.sites.forEachIndexed { i,site ->
                                    SiteCard(site,i,{a.select(site.campsite)})
                                }
                                Text("추천 장소와 거리는 목업 데이터예요. 실제 숙박 허용·충전 가능 정보가 아니에요.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        JourneyStage.HOME_SETUP,JourneyStage.SCHEDULE->Unit
                        JourneyStage.BUDGET->BudgetContent(s,a)
                        JourneyStage.TO_CHARGER,JourneyStage.TO_CAMP,JourneyStage.HOME->RouteContent(s)
                        JourneyStage.CHARGING->ChargingContent(s,a)
                        JourneyStage.STAY->StayContent(s,a,wide)
                        JourneyStage.COMPLETE->Panel {
                            Text("수고했어요. 다음 여행도 함께해요.",style=MaterialTheme.typography.titleLarge)
                            BatteryMeter(s.observedSoc,"도착 배터리")
                            Text("도착은 직접 확인한 상태이며, 잔량은 차량에서 읽었어요.")
                        }
                    }
                    if(s.fault!=null || listOf("부족", "실패", "한계").any { s.message.contains(it) }) Notice(s.message,true)
                }
            }
        }
    }
}

@Composable private fun JourneyProgress(stage:JourneyStage) {
    val current=when(stage){JourneyStage.RECOMMEND,JourneyStage.HOME_SETUP,JourneyStage.SCHEDULE->0;JourneyStage.BUDGET->1;JourneyStage.TO_CHARGER,JourneyStage.CHARGING,JourneyStage.TO_CAMP->2;JourneyStage.STAY->3;else->4}
    FlowRow(horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf("장소·시간","전력","이동","차박","귀가").forEachIndexed { i,label ->
            Text("${i+1}  $label",style=MaterialTheme.typography.bodySmall,fontWeight=if(i==current) FontWeight.Bold else FontWeight.Normal,
                color=if(i==current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable private fun SiteCard(site:TripSite,index:Int,onSelect:()->Unit,modifier:Modifier=Modifier) {
    val border=MaterialTheme.colorScheme.outline.copy(alpha=.35f)
    Surface(modifier=modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .clickable(role=Role.Button,onClick=onSelect),
        shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,border),color=MaterialTheme.colorScheme.surface) {
        Column {
            CampLandscape(index,Modifier.fillMaxWidth().height(170.dp))
            Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(site.campsite.name,style=MaterialTheme.typography.titleLarge)
                Text(site.campsite.landscape,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("시연 거리 "+site.campsite.distance.f()+"km",fontWeight=FontWeight.SemiBold)
                Text(when(index){0->"화장실 / 개수대";1->"샤워실 / 산책로";else->"전망 / 산책로"},style=MaterialTheme.typography.bodySmall)
                Text("이 차박지 선택",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)
            }
        }
    }
}
@Composable private fun BudgetContent(s:CampilotUiState,a:JourneyActions) {
    val b=s.journeyBudget
    if(b==null) {Notice("차량 배터리를 아직 읽지 못했어요. 아래에서 다시 확인해주세요.");return}
    val impossible=!b.feasible || b.chargeKwh>0 && !b.chargerReachable
    Panel {
        StatusPill(if(impossible) "계획 변경 필요" else if(b.chargeKwh>0.001) "충전 1회 필요" else "추가 충전 없이 출발")
        Text(when {
            !b.feasible->"한 번의 충전으로는 전력이 부족해요"
            b.chargeKwh>0 && !b.chargerReachable->"충전소까지 갈 전력이 부족해요"
            b.chargeKwh>0.001->"출발 전에 "+b.chargeKwh.f()+"kWh를 더 충전해요"
            else->"지금 잔량으로 다녀올 수 있어요"
        },style=MaterialTheme.typography.headlineMedium)
        Text("이동과 출발 전까지 "+s.plan.hours.f()+"시간의 사용량, 귀가 예비량을 함께 계산했어요.")
        Text("귀가 목적지 · "+(s.confirmedHome?.label ?: "확인 필요"))
        Text("집까지 약 "+(s.homeKm ?: 0.0).f()+"km는 좌표 기반 추정 거리예요. 실제 도로 거리와 다를 수 있어요.",style=MaterialTheme.typography.bodySmall)
        BatteryMeter(s.observedSoc,"현재 배터리")
        if(!b.feasible) Notice("체류 시간은 유지해요. 이 일정에는 추가 충전 계획이 필요하며, 현재는 한 곳 경유만 지원해 출발 안내를 제공할 수 없어요.",true)
    }
    Panel {
        Text("전력 계산",style=MaterialTheme.typography.headlineMedium)
        Text("예비 배터리를 포함해 필요한 전력",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(b.required.f()+" kWh",style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.primary)
        HorizontalDivider()
        EnergyBreakdownRow("차박지까지 이동",(s.selected.distance/s.plan.efficiency).f()+" kWh")
        EnergyBreakdownRow("차박 중 사용",(s.plan.hours*s.power).f()+" kWh")
        EnergyBreakdownRow("집까지 귀가",((s.homeKm ?: 0.0)/s.plan.efficiency).f()+" kWh")
        EnergyBreakdownRow("예비 배터리 · 10%",b.reserveBattery.f()+" kWh")
        Text("귀가 목적지: "+(s.confirmedHome?.label ?: "확인 필요"))
        Text("귀가 거리 약 "+(s.homeKm ?: 0.0).f()+"km · 좌표 간 직선거리 × 1.3의 임시 추정값이며 실제 도로 거리가 아니에요.",style=MaterialTheme.typography.bodySmall)
        EnergyBreakdownRow("출발 기준 목표",b.targetSoc.f()+"%")
        Text("현재부터 내일 출발까지를 사용 기간으로 잡은 보수적인 목업 계산이에요. 이동 시간이 포함되어 실제 체류 시간과 다를 수 있어요.",style=MaterialTheme.typography.bodySmall)
        Text("평균 "+s.power.f()+"kW 사용 가정입니다. 냉난방 실측값은 아니에요.",style=MaterialTheme.typography.bodySmall)
        Text("예비 배터리는 전체 용량의 10%로 고정하며, 귀가 후 남겨둘 양이에요.",style=MaterialTheme.typography.bodySmall)
    }
    RouteStops(s,includeCharger=b.chargeKwh>0.001)
    TextButton(onClick=a.editHome){Text("귀가할 집 바꾸기")}
    TextButton(onClick=a.editSchedule){Text("출발 시간 바꾸기")}
    TextButton(onClick=a.back){Text("다른 장소 고르기")}
}
@Composable private fun EnergyBreakdownRow(label:String,value:String) {
    Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value,Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
    }
}
@Composable private fun RouteStops(s:CampilotUiState,includeCharger:Boolean=s.viaCharger && !s.charged) {
    Panel {
        val fullJourney=s.stage==JourneyStage.BUDGET
        Text(if(fullJourney) "전체 여정" else "이번 이동",style=MaterialTheme.typography.titleLarge)
        val points=buildList {
            add(if(s.returning) s.selected.name else if(s.charged) "충전소" else "출발지")
            if(includeCharger) add("시연 충전소")
            add(if(s.returning) s.confirmedHome?.label ?: "집 확인 필요" else s.selected.name)
            if(fullJourney) add(s.confirmedHome?.label ?: "집 확인 필요")
        }
        points.forEachIndexed { i,name ->
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                StatusPill((i+1).toString())
                if(fullJourney && i==points.lastIndex) {
                    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Text("집 (귀가)",fontWeight=FontWeight.Bold)
                        Text(name,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else Text(name,fontWeight=if(i==points.lastIndex)FontWeight.Bold else FontWeight.Normal)
            }
        }
        Text("시연용 장소와 경로예요.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun RouteContent(s:CampilotUiState) {
    RouteStops(s)
    Notice("차량 내비게이션에서 안내를 확인하세요. 도착 후 P단에 놓고 아래 버튼을 눌러주세요.")
}
@Composable private fun ChargingContent(s:CampilotUiState,a:JourneyActions) {
    Panel {
        StatusPill("충전 목표 "+s.chargeTarget.f()+"%")
        BatteryMeter(if(s.fresh) s.observedSoc else null,"현재 배터리")
        Text(when {
            !s.fresh -> "배터리를 다시 확인해주세요."
            (s.observedSoc ?: 0.0)>=s.chargeTarget -> "목표에 도달했어요. 다음 장소로 출발하세요."
            else -> "목표 잔량까지 충전한 뒤 다음 장소로 이동해요."
        },style=MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick=a.refresh){Text("잔량 다시 확인")}
        Text("실제 충전 시작과 종료는 차량·충전기에서 진행해주세요.",
            color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun StayContent(s:CampilotUiState,a:JourneyActions,wide:Boolean=false) {
    val battery:@Composable ()->Unit={
        Panel {
            BatteryMeter(if(s.fresh) s.observedSoc else null,"남은 배터리")
            Metric("남은 휴식",s.remaining.f()+"시간")
            val expected=(s.energy-s.remaining*s.power)/s.plan.capacity*100
            if(s.fresh) Metric("차박 종료 예상",expected.f()+"%")
            else Text("예상 잔량은 최신 배터리 확인 후 표시해요.")
            Text("현재 소비 가정으로 계산한 예상이에요.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(!s.fresh) Notice("차량 정보가 오래되었어요. 다시 연결한 뒤 진행해주세요.",true)
            if((s.journeyBudget?.chargeKwh ?: 0.0)>0) Notice("계획보다 전력이 부족할 수 있어요. 체류 시간을 줄이거나 충전을 준비하세요.",true)
        }
    }
    val climate:@Composable ()->Unit={
        Panel {
            Text("편안한 온도",style=MaterialTheme.typography.titleLarge)
            val temp=s.sdk.temperature
            Text(temp?.let{it.f()+"°C"} ?: "사용할 수 없음",fontSize=42.sp,fontWeight=FontWeight.Bold)
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick={a.temperature(-.5)},enabled=s.canSetTemperature,modifier=Modifier.weight(1f).heightIn(min=60.dp)){Text("− 0.5°")}
                OutlinedButton(onClick={a.temperature(.5)},enabled=s.canSetTemperature,modifier=Modifier.weight(1f).heightIn(min=60.dp)){Text("+ 0.5°")}
            }
            Text("설정 범위 "+s.plan.minTemp.f()+"~"+s.plan.maxTemp.f()+"°C",style=MaterialTheme.typography.bodySmall)
            Text("현재는 자동 관리 준비 중이에요. 공조를 지원하는 차량에서 수동 온도 변경을 사용할 수 있어요.",style=MaterialTheme.typography.bodySmall)
            if(s.applying) Text("온도 적용을 확인하고 있어요.")
            if(s.applying) OutlinedButton(onClick=a.stop){Text("즉시 중지")}
        }
    }
    if(wide) Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) {
        Box(Modifier.weight(1f)){battery()};Box(Modifier.weight(1f)){climate()}
    } else {battery();climate()}
}
