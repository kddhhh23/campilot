package com.example.campilot

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class PlaceTimeFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun ready() {
        rule.waitUntil(10_000) { rule.onAllNodesWithText("차박지 추천 받기").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun firstScreenOnlyAsksForPlace() {
        ready()
        rule.onNodeWithText("현재 위치").assertIsDisplayed()
        rule.onNodeWithText("서울시청 인근").assertDoesNotExist()
        rule.onNodeWithText("Pleos 위치 다시 확인").assertExists()
        rule.onNodeWithText("솔숲 캠핑장").assertDoesNotExist()
        rule.onNodeWithText("호수 캠핑장").assertDoesNotExist()
        rule.onNodeWithText("별빛 캠핑장").assertDoesNotExist()
        rule.onNodeWithText("차박지 추천 받기").performClick()
        rule.onNodeWithText("솔숲 캠핑장").assertExists()
        rule.onNodeWithText("호수 캠핑장").assertExists()
        rule.onNodeWithText("별빛 캠핑장").assertExists()
        rule.onNodeWithText("차박지 추천 받기").assertDoesNotExist()
        rule.onNodeWithText("계획 바꾸기").assertDoesNotExist()
        rule.onNodeWithText("설정").assertDoesNotExist()
        rule.onNodeWithText("전력 계획 확인").assertDoesNotExist()
    }
    @Test fun selectedPlaceNeedsHomeConfirmationAndSurvivesRecreation() {
        ready()
        rule.onNodeWithText("차박지 추천 받기").performClick()
        rule.onNodeWithText("호수 캠핑장").performClick()
        rule.onNodeWithText("돌아갈 집을 확인해주세요").assertIsDisplayed()
        rule.onNodeWithText("전력 계획 확인").assertDoesNotExist()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("호수 캠핑장 여정의 귀가 목적지예요.").assertExists()
        rule.onNodeWithText("다른 장소 고르기").performScrollTo().performClick()
        rule.onNodeWithText("솔숲 캠핑장").performClick()
        rule.onNodeWithText("솔숲 캠핑장 여정의 귀가 목적지예요.").assertExists()
    }
}
