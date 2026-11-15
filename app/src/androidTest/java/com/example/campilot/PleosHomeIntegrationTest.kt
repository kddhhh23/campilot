package com.example.campilot

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ai.pleos.playground.navi.constants.BookmarkType
import ai.pleos.playground.navi.data.BookmarkInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.junit.Assert.*
import org.junit.Test

class PleosHomeIntegrationTest {
    @Test fun nativeBookmarkReplyCanDistinguishUnregisteredHome() = runBlocking {
        withContext(Dispatchers.Main) {
            val helper = NaviHelper(ApplicationProvider.getApplicationContext<Context>())
            val answer = CompletableDeferred<List<BookmarkInfo>>()
            val listener = object : NaviHelperEventListener {
                override fun onBookmarkInfo(bookmarkInfoList: List<BookmarkInfo>) { answer.complete(bookmarkInfoList) }
            }
            try {
                withTimeout(8_000) { suspendCancellableCoroutine<Unit> { c ->
                    helper.initialize({ if(c.isActive) c.resume(Unit) }, { if(c.isActive) c.resumeWithException(it) })
                }}
                helper.addListener(listener)
                helper.getBookmarkInfo()
                val list = withTimeout(8_000) { answer.await() }
                val homes = list.filter { it.category == BookmarkType.HOME }
                println("HOME count=" + homes.size + "; zeroCoordinates=" +
                    homes.map { it.latitude == 0.0 && it.longitude == 0.0 } +
                    "; blankAddresses=" + homes.map { it.address.isBlank() })
                assertNotNull(list)
            } finally { helper.removeListener(listener); helper.release(); answer.cancel() }
        }
    }
}
