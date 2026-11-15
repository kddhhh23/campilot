package com.example.campilot
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
class CampilotFlowTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    private fun text(value:String)=rule.onNodeWithText(value)
    private fun ready() {
        rule.waitUntil(10000){rule.onAllNodesWithText("차박지 추천 받기").fetchSemanticsNodes().isNotEmpty()}
    }
    @Test fun invalidInputBlocksContinue() {
        ready();rule.runOnIdle { rule.activity.showPlanEditorForTest() }
        text("체류 시간 (h)").performTextClearance()
        text("설정 저장").assertIsNotEnabled()
    }
    @Test fun selectionSurvivesRecreation() {
        ready()
        text("차박지 추천 받기").performClick()
        text("호수 캠핑장").performClick()
        rule.activityRule.scenario.recreate()
        text("호수 캠핑장 여정의 귀가 목적지예요.").assertExists()
    }
}
