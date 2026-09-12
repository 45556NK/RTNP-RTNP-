// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/LifetimeBarRenderTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.rtnp.demo.ui.panel.data.LifetimeBarComponent

object LifetimeBarRenderTool {

    fun draw(canvas: Canvas, comp: LifetimeBarComponent, panelRect: RectF) {
        val remaining = (comp.maxLifetime - comp.currentLifetime).coerceIn(0f, comp.maxLifetime)
        val ratio = if (comp.maxLifetime > 0f) remaining / comp.maxLifetime else 0f
        ProgressBarRenderTool.draw(
            canvas, panelRect, comp.layoutY, comp.computedHeight,
            ratio, Color.argb(180, 180, 180, 180),
            "${remaining.toInt()}/${comp.maxLifetime.toInt()}"
        )
    }
}