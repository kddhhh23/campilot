package com.example.campilot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.campilot.f
import com.example.campilot.domain.*

@Composable private fun NumberField(label:String,value:String,change:(String)->Unit) {
    OutlinedTextField(value=value,onValueChange=change,label={Text(label)},singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())
}
@Composable fun PlanScreen(p:CampPlan,onContinue:(CampPlan)->Unit) {
    var hours by rememberSaveable(p){mutableStateOf(p.hours.f())}
    var capacity by rememberSaveable(p){mutableStateOf(p.capacity.f())}
    var efficiency by rememberSaveable(p){mutableStateOf(p.efficiency.f())}
    var min by rememberSaveable(p){mutableStateOf(p.minTemp.f())}
    var max by rememberSaveable(p){mutableStateOf(p.maxTemp.f())}
    var cooling by rememberSaveable(p){mutableStateOf(p.cooling)}
    fun parse(v:String)=v.trim().toDoubleOrNull()?.takeIf{it.isFinite()}
    val values=listOf(hours,capacity,efficiency,min,max).map{parse(it)}
    val entered=if(values.all{it!=null}) p.copy(hours=values[0]!!,capacity=values[1]!!,
        efficiency=values[2]!!,minTemp=values[3]!!,maxTemp=values[4]!!,cooling=cooling) else null
    val draft=entered?.let { if(it.minTemp<=it.maxTemp)
        it.copy(temperature=it.temperature.coerceIn(it.minTemp,it.maxTemp)) else it }
    val valid=draft!=null && draft.hours>0 && draft.minTemp>=16 && draft.maxTemp<=30 && EnergyCalculator.calculate(draft,0.0) is Budget.Valid
    Scaffold(bottomBar={Surface(shadowElevation=6.dp) {Box(Modifier.imePadding().navigationBarsPadding().padding(16.dp)) {
        PrimaryAction("설정 저장",valid){onContinue(draft!!)}
    }}}) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement=Arrangement.spacedBy(24.dp)) {
            Text("오늘 밤의 계획",style=MaterialTheme.typography.headlineLarge)
            Text("머무를 시간만 정하면 나머지는 기본값으로 준비해요.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            Panel {
                Text("얼마나 머무를까요?",style=MaterialTheme.typography.titleLarge)
                NumberField("체류 시간 (h)",hours){hours=it}
                if(parse(hours)==null || (parse(hours) ?: 0.0)<=0) Text("0보다 큰 시간을 입력해주세요.",color=MaterialTheme.colorScheme.error)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    listOf(4,8,12).forEach { h -> FilterChip(selected=parse(hours)==h.toDouble(),onClick={hours=h.toString()},label={Text("${h}시간")}) }
                }
            }
            Disclosure("온도 설정") {
                Row {Text(if(cooling) "냉방" else "난방",Modifier.weight(1f));Switch(cooling,{cooling=it})}
                NumberField("최저 허용 온도 (°C)",min){min=it}
                NumberField("최고 허용 온도 (°C)",max){max=it}
                Text("16~30°C 안에서 편안한 범위를 정해주세요.",style=MaterialTheme.typography.bodySmall)
            }
            Disclosure("고급 설정") {
                Text("시연용 전력 계산에 사용하는 값입니다.",style=MaterialTheme.typography.bodySmall)
                NumberField("차량 용량 (kWh)",capacity){capacity=it}
                NumberField("전비 (km/kWh)",efficiency){efficiency=it}
                Text("예비 배터리 · 10% 고정")
                Text("차량 연결 시 잔량은 실제 차량 값을 사용합니다. 평균 소비는 1.2kW로 가정합니다.",style=MaterialTheme.typography.bodySmall)
            }
            if(!valid) Notice("입력값을 확인해주세요. 최저 온도는 최고 온도보다 높을 수 없습니다.",true)
        }
    }
}
