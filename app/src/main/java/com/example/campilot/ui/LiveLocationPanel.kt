package com.example.campilot.ui

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.campilot.location.PleosLocationFix
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest

/** Only Pleos NaviHelper supplies coordinates. No Android LocationManager or fallback position. */
@Composable fun LiveLocationPanel() {
    val context = LocalContext.current.applicationContext
    val owner = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }
    var fix by remember { mutableStateOf<PleosLocationFix?>(null) }
    var detail by remember { mutableStateOf("Pleos 차량 위치를 연결하고 있어요.") }
    LaunchedEffect(context, owner, refresh) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            fix = null
            detail = "Pleos 차량 위치를 연결하고 있어요."
            callbackFlow<Pair<PleosLocationFix?, Boolean>> {
                trySend(null to false)
                val helper = NaviHelper(context)
                val listener = object : NaviHelperEventListener {
                    override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
                        trySend(PleosLocationFix.from(currentLocationInfo.latitude,
                            currentLocationInfo.longitude, currentLocationInfo.address) to false)
                    }
                    override fun onError(errorCode: NaviErrorCode) {
                        trySend(null to true)
                    }
                }
                var connected = false
                try {
                    helper.initialize(onConnected@{
                        if (!isActive) return@onConnected
                        connected = true
                        helper.addListener(listener)
                        // Request only information: this never launches route guidance.
                        try { helper.getCurrentLocationInfo() }
                        catch (_: Exception) { trySend(null to true) }
                        Unit
                    }, { trySend(null to true); Unit })
                } catch (_: Exception) { trySend(null to true) }
                val query = launch {
                    while (isActive) {
                        delay(5_000)
                        try { if (connected) helper.getCurrentLocationInfo() }
                        catch (_: Exception) { trySend(null to true) }
                    }
                }
                awaitClose {
                    query.cancel()
                    helper.removeListener(listener)
                    helper.release()
                }
            }.collectLatest { (received, error) ->
                fix = received
                detail = when {
                    error -> "Pleos 위치 연결을 확인하지 못했어요. 다시 시도해주세요."
                    received == null -> "Pleos에서 유효한 현재 위치를 아직 보내지 않았어요."
                    else -> "Pleos 차량 위치"
                }
                // A silent provider must not leave an old coordinate on screen indefinitely.
                delay(15_000)
                fix = null
                detail = "Pleos 위치 응답을 기다리고 있어요. 다시 확인해주세요."
            }
        }
    }
    Panel {
        Text("현재 위치", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        fix?.let {
            if (it.address.isNotBlank()) Text(it.address, style = MaterialTheme.typography.titleLarge)
            Text(it.coordinates, style = MaterialTheme.typography.titleMedium)
        }
        Text(detail, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { refresh++ }) { Text("Pleos 위치 다시 확인") }
    }
}
