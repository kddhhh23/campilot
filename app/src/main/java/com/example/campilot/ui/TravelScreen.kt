package com.example.campilot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campilot.domain.JourneyStage

/** Content has no Activity/SDK dependency: the host owns window transitions. */
@Composable fun TravelScreen(s: CampilotUiState, a: JourneyActions) {
    val destination = when (s.stage) {
        JourneyStage.TO_CHARGER -> "경유 충전소"
        JourneyStage.HOME -> s.confirmedHome?.label ?: "집"
        else -> s.selected.name
    }
    val arrived = s.navigationArrived
    val parked = s.fresh && s.sdk.parked == true
    val pending = s.pendingRoute != null
    val title = when {
        pending -> "내비 경로를 확인해주세요"
        s.busy -> "경로를 연결하고 있어요"
        s.navigationEnded -> "안내를 종료했어요"
        s.navigationProblem != null -> "내비 연결을 확인해주세요"
        arrived -> when (s.stage) {
            JourneyStage.TO_CHARGER -> "충전소에 도착했어요"
            JourneyStage.HOME -> "집에 도착했어요"
            else -> "차박지에 도착했어요"
        }
        else -> when (s.stage) {
            JourneyStage.TO_CHARGER -> "충전소로 이동 중"
            JourneyStage.HOME -> "집으로 이동 중"
            else -> "차박지로 이동 중"
        }
    }
    Scaffold(bottomBar = {
        if (pending || arrived || s.navigationProblem != null) {
            Surface {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        pending -> PrimaryAction("경로 확인했어요", !s.busy, a.confirmRoute)
                        s.navigationEnded && s.stage == JourneyStage.TO_CAMP -> {
                            if (!parked) Text("안전하게 주차한 뒤 P단으로 변경해주세요.")
                            PrimaryAction("차박 시작", s.canStartStay, a.startStay)
                            OutlinedButton(onClick = a.retryNavigation, enabled = !s.busy,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("다시 안내받기") }
                        }
                        s.navigationProblem != null -> PrimaryAction(if(s.navigationEnded) "다시 안내받기" else "경로 다시 연결", !s.busy, a.retryNavigation)
                        else -> {
                            if (!parked) Text("안전하게 주차한 뒤 P단으로 변경해주세요.")
                            PrimaryAction(when (s.stage) {
                                JourneyStage.TO_CHARGER -> "충전 상태 확인"
                                JourneyStage.HOME -> "귀가 완료"
                                else -> "차박 시작"
                            }, parked && !s.busy) {
                                when (s.stage) {
                                    JourneyStage.TO_CHARGER -> a.arriveCharger()
                                    JourneyStage.HOME -> a.arriveHome()
                                    else -> a.startStay()
                                }
                            }
                        }
                    }
                }
            }
        }
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            StatusPill(if (arrived) "도착" else if(s.navigationEnded) "안내 종료" else "이동")
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Text(destination, style = MaterialTheme.typography.titleLarge)
            if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(when {
                pending -> "내비게이션의 목적지와 경유지를 확인한 뒤 눌러주세요."
                s.navigationProblem != null -> s.navigationProblem
                arrived -> "Pleos 내비게이션에서 도착 알림을 받았어요."
                else -> "경로는 내비게이션에서 확인하세요. 도착 알림을 받으면 차박 준비 화면으로 전환해요."
            }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (s.routeError) Notice("경로를 연결하지 못했어요. 다시 시도해주세요.", true)
        }
    }
}
