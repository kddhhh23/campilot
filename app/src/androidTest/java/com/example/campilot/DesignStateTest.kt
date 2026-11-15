package com.example.campilot
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.campilot.domain.*
import com.example.campilot.ui.*
import com.example.campilot.vehicle.PleosSnapshot
import org.junit.Rule
import org.junit.Test

class DesignStateTest {
    @get:Rule val rule=createComposeRule()
    @Test fun vehicleModeCanSaveRangeWithoutHiddenStartTemperature() {
        rule.setContent { CampilotTheme { PlanScreen(CampPlan()){} } }
        rule.onNodeWithText("온도 설정").performScrollTo().performClick()
        rule.onNodeWithText("최고 허용 온도 (°C)").performScrollTo().performTextReplacement("26")
        rule.onNodeWithText("최저 허용 온도 (°C)").performScrollTo().performTextReplacement("24")
        rule.onNodeWithText("설정 저장").assertIsEnabled()
    }
    @Test fun staleChargeNeverInvitesDepartureAndStaleStayHasNoForecast() {
        var state by mutableStateOf(CampilotUiState(stage=JourneyStage.CHARGING,chargeTarget=43.0,
            sdk=PleosSnapshot(soc=90.0,sampledAt=1)))
        rule.setContent { CampilotTheme { JourneyScreen(state,JourneyActions()) } }
        rule.onNodeWithText("배터리를 다시 확인해주세요.").assertExists()
        rule.onNodeWithText("목표에 도달했어요. 다음 장소로 출발하세요.").assertDoesNotExist()
        rule.onNodeWithText("캠핑장으로 출발").assertIsNotEnabled()
        rule.runOnIdle { state=state.copy(stage=JourneyStage.STAY,phase=TripPhase.STAY) }
        rule.onNodeWithText("예상 잔량은 최신 배터리 확인 후 표시해요.").assertExists()
    }
    @Test fun englishRouteErrorHasVisibleRecovery() {
        rule.setContent { CampilotTheme { JourneyScreen(
            CampilotUiState(stage=JourneyStage.BUDGET,energy=36.0,
                routeError=true,routeMessage="Service not connected"),JourneyActions()) } }
        rule.onNodeWithText("경로를 준비하지 못했어요. 연결 상태를 확인하고 다시 시도해주세요.").assertIsDisplayed()
    }
}
