package com.example.campilot

import androidx.activity.compose.setContent
import com.example.campilot.domain.CampPlan
import com.example.campilot.ui.CampilotTheme
import com.example.campilot.ui.PlanScreen

fun MainActivity.showPlanEditorForTest() {
    setContent { CampilotTheme { PlanScreen(CampPlan()) {} } }
}
