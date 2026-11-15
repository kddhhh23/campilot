package com.example.campilot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable fun HomeScreen(s: CampilotUiState, a: JourneyActions) {
    val action = when {
        s.homeLoading -> "집 정보 확인 중…"
        s.homeCandidate != null -> "이 주소로 귀가할게요"
        s.homeError -> "집 정보 다시 불러오기"
        else -> "내비에서 집 등록"
    }
    Scaffold(bottomBar = {
        Surface(shadowElevation = 8.dp) {
            Box(Modifier.navigationBarsPadding().padding(24.dp)) {
                PrimaryAction(action, !s.homeLoading) {
                    when {
                        s.homeCandidate != null -> a.confirmHome()
                        s.homeError -> a.refreshHome()
                        else -> a.registerHome()
                    }
                }
            }
        }
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("귀가 목적지", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text("돌아갈 집을 확인해주세요", style = MaterialTheme.typography.headlineLarge)
            Text(s.selected.name + " 여정의 귀가 목적지예요.")
            Panel {
                Text(s.homeStatus, style = MaterialTheme.typography.titleLarge)
                if (s.homeLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                s.homeCandidate?.let {
                    Text(it.label, style = MaterialTheme.typography.headlineMedium)
                    Text(String.format(Locale.KOREA, "위도 %.5f · 경도 %.5f", it.point.latitude, it.point.longitude),
                        style = MaterialTheme.typography.bodySmall)
                    Text("Pleos 내비게이션에 저장된 집 정보예요.", style = MaterialTheme.typography.bodySmall)
                }
                if (!s.homeLoading && !s.homeError) TextButton(onClick = a.refreshHome) { Text("집 정보 다시 불러오기") }
            }
            Text("집 주소를 확인한 뒤 귀가에 필요한 전력까지 계산해요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!s.homeLoading && s.homeCandidate == null && s.homeError)
                TextButton(onClick = a.registerHome) { Text("내비에서 집 등록") }
            TextButton(onClick = a.back) { Text("다른 장소 고르기") }
        }
    }
}
