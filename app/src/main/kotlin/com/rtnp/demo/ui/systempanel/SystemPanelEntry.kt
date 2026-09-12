package com.rtnp.demo.ui.systempanel

import android.graphics.Canvas
import android.view.MotionEvent
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.ui.FloatingWindowTool

object SystemPanelEntry {

    private var isVisible = false

    fun show(startX: Float, startY: Float, unitSystem: UnitSystem?) {
        isVisible = true
        FloatingWindowTool.show(startX = startX, startY = startY)
        SystemPanelLogic.onShow(unitSystem)

        FloatingWindowTool.onOutsideClick = {
            hide()
        }
    }

    fun hide() {
        isVisible = false
        FloatingWindowTool.hide()
        SystemPanelLogic.onHide()
        FloatingWindowTool.onOutsideClick = null
    }

    fun isShowing(): Boolean = isVisible

    fun draw(canvas: Canvas) {
        // 悬浮窗与信息框已由 GameView 统一绘制
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isVisible) return false
        return FloatingWindowTool.onTouchEvent(event)
    }

    fun onSizeChanged(w: Int, h: Int) {
        FloatingWindowTool.onSizeChanged(w, h)
        SystemPanelLogic.onSizeChanged(w, h)
    }
}