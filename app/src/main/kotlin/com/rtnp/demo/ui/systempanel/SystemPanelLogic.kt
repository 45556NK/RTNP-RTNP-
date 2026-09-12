package com.rtnp.demo.ui.systempanel

import android.graphics.Canvas
import android.view.MotionEvent
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.FloatingWindowTool

object SystemPanelLogic {

    private var screenWidth = 0
    private var screenHeight = 0
    private var unitSystem: UnitSystem? = null
    private var currentUnit: PlacedObject? = null
    private var contentComponent: SystemPanelContentComponent? = null

    fun onShow(us: UnitSystem?) {
        unitSystem = us
        currentUnit = us?.selectedUnit

        if (contentComponent == null) {
            contentComponent = SystemPanelContentComponent()
            FloatingWindowTool.register(contentComponent!!)
        }
        contentComponent?.unit = currentUnit
        contentComponent?.screenWidth = screenWidth
        contentComponent?.screenHeight = screenHeight
    }

    fun onHide() {
        contentComponent?.cleanup()
        contentComponent?.let { FloatingWindowTool.unregister(it) }
        contentComponent = null
        currentUnit = null
        unitSystem = null
    }

    fun onSizeChanged(w: Int, h: Int) {
        screenWidth = w
        screenHeight = h
        contentComponent?.screenWidth = w
        contentComponent?.screenHeight = h
    }

    fun draw(canvas: Canvas) {
        // 悬浮窗统一渲染
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        // 悬浮窗统一分发
        return false
    }
    fun getUnitSystem(): UnitSystem? = unitSystem
}