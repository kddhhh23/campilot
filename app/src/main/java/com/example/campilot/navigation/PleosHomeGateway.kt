package com.example.campilot.navigation

import android.content.Context
import ai.pleos.playground.navi.constants.BookmarkType
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.BookmarkInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import com.example.campilot.domain.HomeDestination
import com.example.campilot.domain.TripPoint
import com.example.campilot.location.PleosLocationFix
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface HomeGateway {
    suspend fun readHome(): Result<HomeDestination?>
    suspend fun openHomeRegistration(): Result<Unit>
}

class PleosHomeGateway(private val context: Context) : HomeGateway {
    private suspend fun <T> connected(block: suspend (NaviHelper) -> T): Result<T> {
        val helper = NaviHelper(context.applicationContext)
        return try {
            withTimeout(8_000) {
                suspendCancellableCoroutine<Unit> { c ->
                    helper.initialize({ if (c.isActive) c.resume(Unit) },
                        { if (c.isActive) c.resumeWithException(it) })
                }
            }
            Result.success(block(helper))
        } catch (_: TimeoutCancellationException) {
            Result.failure(IllegalStateException("내비게이션 집 정보 응답 시간이 초과됐어요."))
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { Result.failure(e)
        } finally { helper.release() }
    }

    /** null means a received bookmark list contained no HOME, not a timeout or connection failure. */
    override suspend fun readHome(): Result<HomeDestination?> = connected { helper ->
        val answer = CompletableDeferred<HomeDestination?>()
        val listener = object : NaviHelperEventListener {
            override fun onBookmarkInfo(bookmarkInfoList: List<BookmarkInfo>) {
                val home = bookmarkInfoList.firstOrNull { it.category == BookmarkType.HOME }
                if (home == null || (home.latitude == 0.0 && home.longitude == 0.0 && home.address.isBlank())) {
                    answer.complete(null); return
                }
                val fix = PleosLocationFix.from(home.latitude, home.longitude, home.address)
                if (fix == null) {
                    answer.completeExceptionally(IllegalStateException("등록된 집 좌표를 확인할 수 없어요."))
                } else answer.complete(HomeDestination(
                    TripPoint(home.name.ifBlank { "집" }, fix.latitude, fix.longitude), home.address.trim()))
            }
            override fun onError(errorCode: NaviErrorCode) {
                answer.completeExceptionally(IllegalStateException("집 정보를 불러오지 못했어요 · " + errorCode))
            }
        }
        helper.addListener(listener)
        try {
            helper.getBookmarkInfo()
            withTimeout(8_000) { answer.await() }
        } finally { helper.removeListener(listener); answer.cancel() }
    }

    /** Successful return means request sent; screen opening is verified in the native app. */
    override suspend fun openHomeRegistration(): Result<Unit> = connected {
        it.requestNavigationScreen(NavigationScreenRequest.AddHomeFavorite)
    }
}
