package com.example.campilot

import com.example.campilot.domain.HomeDestination
import com.example.campilot.domain.TripPoint
import com.example.campilot.navigation.HomeGateway

class TestHomeGateway : HomeGateway {
    override suspend fun readHome() = Result.success<HomeDestination?>(
        HomeDestination(TripPoint("테스트 집", 37.5665, 126.978), "테스트 귀가 목적지"))
    override suspend fun openHomeRegistration() = Result.success(Unit)
}
