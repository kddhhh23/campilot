package com.example.campilot

import android.os.Bundle
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.example.campilot.domain.*
import com.example.campilot.ui.*
import java.util.Locale

class MainActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(this)[CampilotViewModel::class.java]
        // Keep only the explicit navigation handoff alive while native navigation covers us.
        // Compose collection intentionally pauses when this Activity is not visible.
        lifecycleScope.launch {
            var previousReturn = false
            vm.uiState.map { it.travelWindow to (it.navigationArrived || it.navigationEnded) }
                .distinctUntilChanged().collect { (travel, returnToApp) ->
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                if(travel) vm.showNavigation()
                if(returnToApp && !previousReturn) {
                    startActivity(Intent(this@MainActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                }
                previousReturn = returnToApp
            }
        }
        setContent {
            val s by vm.uiState.collectAsStateWithLifecycle()
            CampilotApp(s,vm){moveTaskToBack(true)}
        }
    }
    override fun onStop(){super.onStop(); androidx.lifecycle.ViewModelProvider(this)[CampilotViewModel::class.java].background()}
    override fun onStart(){super.onStart();androidx.lifecycle.ViewModelProvider(this)[CampilotViewModel::class.java].resume()}
}
fun Double.f()=String.format(Locale.KOREA,"%.1f",this)

@Composable fun CampilotApp(s:CampilotUiState,vm:CampilotViewModel,onRootBack:()->Unit) {
    var settings by rememberSaveable { mutableStateOf(false) }
    BackHandler {
        when {
            settings -> settings=false
            s.stage==JourneyStage.HOME_SETUP -> vm.backToRecommendations()
            s.stage==JourneyStage.SCHEDULE -> vm.editHome()
            s.stage==JourneyStage.BUDGET -> vm.backToSchedule()
            else -> onRootBack()
        }
    }
    CampilotTheme {
        Scaffold(topBar={
            Surface(color=MaterialTheme.colorScheme.background) {
                FlowRow(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=24.dp,vertical=8.dp),
                    horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment=Alignment.CenterVertically) {
                    Text("Campilot",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,
                        modifier=Modifier.padding(end=20.dp))
                    if(settings) TextButton(onClick={settings=false}) { Text("닫기") }
                }
            }
        }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when {
                    !s.loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    settings -> PlanScreen(s.plan) { vm.updatePlan(it);settings=false }
                    else -> JourneyScreen(s,JourneyActions(
                        recommend=vm::requestRecommendations,select=vm::select,depart=vm::depart,arriveCharger=vm::arriveCharger,
                        continueAfterCharge=vm::continueAfterCharge,
                        startStay=vm::startStay,home=vm::goHome,arriveHome=vm::arriveHome,
                        stop={vm.stop()},temperature=vm::manualTemperature,
                        reset=vm::reset,
                        refresh=vm::refreshVehicle,settings={settings=true},
                        back=vm::backToRecommendations,confirmRoute=vm::confirmRoute,retryNavigation=vm::retryNavigation,
                        refreshHome=vm::refreshHome,confirmHome=vm::confirmHome,
                        registerHome=vm::registerHome,editHome=vm::editHome,
                        chooseTime=vm::chooseDepartureTime,confirmSchedule=vm::confirmSchedule,
                        editSchedule=vm::backToSchedule))
                }
            }
        }
    }
}
