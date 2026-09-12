// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/SpeedBarRenderTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.rtnp.demo.ui.panel.data.SpeedBarComponent

object SpeedBarRenderTool {

    fun draw(canvas: Canvas, comp: SpeedBarComponent, panelRect: RectF) {
        val ratio = if (comp.maxSpeed > 0f) (comp.currentSpeed / comp.maxSpeed).coerceIn(0f, 1f) else 0f
        ProgressBarRenderTool.draw(
            canvas, panelRect, comp.layoutY, comp.computedHeight,
            ratio, Color.rgb(0, 180, 180),
            "${comp.currentSpeed.toInt()}/${comp.maxSpeed.toInt()}"
        )
    }
}