package com.example.campilot
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class FirstUseTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun firstScreenHasClearNextActionAndOptionalDetails() {
        rule.waitUntil(10_000) { rule.onAllNodesWithText("차박지 추천 받기").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("차박지 추천 받기").assertIsDisplayed()
        rule.onNodeWithText("차박을 준비해볼까요?").assertIsDisplayed()
        rule.onNodeWithText("차량 용량 (kWh)").assertDoesNotExist()
        rule.onNodeWithText("차량 연결").assertDoesNotExist()
        rule.onNodeWithText("시연해 보기").assertDoesNotExist()
        rule.onNodeWithText("어둡게").assertDoesNotExist()
    }
    @Test fun settingsStartWithStayDurationAndKeepAdvancedFieldsClosed() {
        rule.waitUntil(10_000) { rule.onAllNodesWithText("차박지 추천 받기").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { rule.activity.showPlanEditorForTest() }
        rule.onNodeWithText("체류 시간 (h)").assertIsDisplayed()
        rule.onNodeWithText("차량 용량 (kWh)").assertDoesNotExist()
        rule.onNodeWithText("고급 설정").performScrollTo().performClick()
        rule.onNodeWithText("차량 용량 (kWh)").performScrollTo().assertIsDisplayed()
    }
}
