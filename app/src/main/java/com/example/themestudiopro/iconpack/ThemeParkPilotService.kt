package com.example.themestudiopro.iconpack

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeParkPilotService : AccessibilityService() {

    sealed class PilotState {
        object Idle : PilotState()
        data class Running(val step: String) : PilotState()
        data class Completed(val message: String) : PilotState()
        data class Error(val error: String) : PilotState()
    }

    companion object {
        private val _pilotState = MutableStateFlow<PilotState>(PilotState.Idle)
        val pilotState: StateFlow<PilotState> = _pilotState

        fun startPilot(targetPackName: String) {
            _pilotState.value = PilotState.Running("Opening Theme Park for pack: $targetPackName")
        }

        fun stopPilot() {
            _pilotState.value = PilotState.Idle
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val currentState = _pilotState.value
        if (currentState !is PilotState.Running) return

        val pkgName = event?.packageName?.toString() ?: return
        if (pkgName.contains("themedesigner") || pkgName.contains("goodlock")) {
            val rootNode = rootInActiveWindow ?: return
            inspectAndAssist(rootNode)
        }
    }

    private fun inspectAndAssist(root: AccessibilityNodeInfo) {
        // Safe offline navigation assistance: detect "Icon" tab or "Create new"
        val iconNodes = root.findAccessibilityNodeInfosByText("Icon")
        if (iconNodes.isNotEmpty()) {
            for (node in iconNodes) {
                if (node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    _pilotState.value = PilotState.Running("Selecting Icon category")
                    break
                }
            }
        }
    }

    override fun onInterrupt() {
        _pilotState.value = PilotState.Idle
    }

    override fun onDestroy() {
        super.onDestroy()
        _pilotState.value = PilotState.Idle
    }
}
