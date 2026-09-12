// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/LifetimeBarTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Paint
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.data.LifetimeBarComponent

object LifetimeBarTool {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 55f
    }

    fun create(unit: PlacedObject): LifetimeBarComponent? {
        if (unit.maxLifetime <= 0) return null
        val remaining = (unit.maxLifetime - unit.lifetime).toInt().coerceAtLeast(0)
        val label = "寿命: ${remaining}/${unit.maxLifetime.toInt()}"
        val textWidth = textPaint.measureText(label) + 32f
        return LifetimeBarComponent(
            currentLifetime = unit.lifetime,
            maxLifetime = unit.maxLifetime,
            tooltipText = label,
            tooltipWidth = textWidth,
            tooltipHeight = textPaint.textSize * 1.5f
        )
    }
}