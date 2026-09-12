// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/HealthBarRenderTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.rtnp.demo.ui.panel.data.HealthBarComponent

object HealthBarRenderTool {

    fun draw(canvas: Canvas, comp: HealthBarComponent, panelRect: RectF) {
        val ratio = (comp.currentHealth / comp.maxHealth).coerceIn(0f, 1f)
        val color = when {
            ratio > 0.6f -> Color.rgb(0, 200, 0)
            ratio > 0.3f -> Color.rgb(255, 200, 0)
            else -> Color.rgb(255, 50, 50)
        }
        ProgressBarRenderTool.draw(
            canvas, panelRect, comp.layoutY, comp.computedHeight,
            ratio, color,
            "${comp.currentHealth.toInt()}/${comp.maxHealth.toInt()}"
        )
    }
}