package com.example.campilot
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.campilot.domain.*
import com.example.campilot.ui.*
import org.junit.Rule
import org.junit.Test
class NarrowLayoutTest {
    @get:Rule val rule=createComposeRule()
    @Test fun narrowScheduleAtLargeFontKeepsTimePickerAndContinueAccessible() {
        rule.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.5f)) {
                CampilotTheme { Box(Modifier.width(375.dp)) {
                    JourneyScreen(CampilotUiState(stage=JourneyStage.SCHEDULE),JourneyActions())
                }}
            }
        }
        rule.onNodeWithText("전력 계획 확인").assertIsDisplayed()
        rule.onNodeWithText("시간 직접 선택").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("장소 다시 고르기").performScrollTo().assertIsDisplayed()
    }
    @Test fun narrowDashboardAtLargeFontKeepsActionsAccessible() {
        rule.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.5f)) {
                CampilotTheme { Box(Modifier.width(375.dp)) {
                    JourneyScreen(CampilotUiState(stage=JourneyStage.STAY,phase=TripPhase.STAY,energy=30.0),JourneyActions())
                }}
            }
        }
        rule.onNodeWithText("귀가 시작").assertIsDisplayed()
        rule.onNodeWithText("편안한 온도").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("여정 상세").performScrollTo().performClick()
        rule.onNodeWithText("여정 끝내기").performScrollTo().performClick()
        rule.onNodeWithText("이 여정을 끝낼까요?").assertIsDisplayed()
        rule.onNodeWithText("계속하기").performClick()
        rule.onNodeWithText("귀가 시작").assertIsDisplayed()
    }
}
