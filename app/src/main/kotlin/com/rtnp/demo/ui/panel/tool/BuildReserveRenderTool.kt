package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.rtnp.demo.ui.panel.data.BuildReserveComponent

object BuildReserveRenderTool {

    fun draw(canvas: Canvas, comp: BuildReserveComponent, panelRect: RectF) {
        val ratio = if (comp.maxReserve > 0) (comp.currentReserve.toFloat() / comp.maxReserve).coerceIn(0f, 1f) else 0f
        val percent = (ratio * 100).toInt()
        ProgressBarRenderTool.draw(
            canvas,
            panelRect,
            comp.layoutY,
            comp.computedHeight,
            ratio,
            Color.rgb(139, 69, 19),   // 棕色
            "$percent%"
        )
    }
}