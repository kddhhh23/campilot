package com.example.campilot.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campilot.domain.DepartureSchedule
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateLabel = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
private val clockLabel = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)

@Composable fun DepartureScreen(s: CampilotUiState, a: JourneyActions) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val chooseTime by rememberUpdatedState(a.chooseTime)
    if (showPicker) DisposableEffect(context) {
        val picker = TimePickerDialog(context, { _, hour, minute ->
            chooseTime(hour * 60 + minute)
            showPicker = false
        }, s.departureMinuteOfDay / 60, s.departureMinuteOfDay % 60, true)
        picker.setTitle("내일 집으로 출발할 시간")
        picker.setOnDismissListener { showPicker = false }
        picker.show()
        onDispose { picker.setOnDismissListener(null); picker.dismiss() }
    }
    Scaffold(bottomBar = {
        Surface(shadowElevation = 8.dp) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("집에 도착하는 시간이 아니라, 차박지에서 출발하는 시간이에요.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryAction("전력 계획 확인", enabled = s.confirmedHome != null, onClick = a.confirmSchedule)
            }
        }
    }) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val wide = maxWidth >= 900.dp && LocalDensity.current.fontScale < 1.4f
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (wide) 32.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("2  시간 정하기", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text("내일, 집으로 몇 시에 출발할까요?", style = MaterialTheme.typography.headlineLarge)
                Text(s.selected.name + "에서 보낼 시간을 정해요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                DepartureContent(s.scheduleNow, s.departureMinuteOfDay, wide, a.chooseTime) { showPicker = true }
                Text("귀가 목적지 · " + (s.confirmedHome?.label ?: "집 확인 필요"), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = a.editHome) { Text("귀가할 집 바꾸기") }
                TextButton(onClick = a.back) { Text("장소 다시 고르기") }
            }
        }
    }
}

/** Renderable without a ViewModel, vehicle connection, or Android dialog. */
@Composable fun DepartureContent(
    now: ZonedDateTime,
    minuteOfDay: Int,
    wide: Boolean,
    onChoose: (Int) -> Unit,
    onOpenPicker: () -> Unit
) {
    val departure = DepartureSchedule.from(now, minuteOfDay).departure
    val current: @Composable () -> Unit = {
        Panel {
            Text("현재 시각", style = MaterialTheme.typography.titleMedium)
            Text(now.format(clockLabel), fontSize = 52.sp, fontWeight = FontWeight.Bold)
            Text("오늘 · " + now.format(dateLabel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("기기에 설정된 현지 시각이에요.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    val choice: @Composable () -> Unit = {
        Panel {
            StatusPill("내일 · " + departure.format(dateLabel))
            Text("집으로 출발", style = MaterialTheme.typography.titleMedium)
            Text(departure.format(clockLabel), fontSize = 64.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { contentDescription = "선택한 출발 시간 " + departure.format(clockLabel) })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(9, 10, 11).forEach { hour ->
                    FilterChip(selected = minuteOfDay == hour * 60, onClick = { onChoose(hour * 60) },
                        label = { Text("오전 " + hour + "시") }, modifier = Modifier.heightIn(min = 56.dp))
                }
            }
            OutlinedButton(onClick = onOpenPicker, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Text("시간 직접 선택")
            }
        }
    }
    if (wide) Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.weight(1f)) { current() }
        Box(Modifier.weight(1.4f)) { choice() }
    } else {
        current()
        choice()
    }
}
