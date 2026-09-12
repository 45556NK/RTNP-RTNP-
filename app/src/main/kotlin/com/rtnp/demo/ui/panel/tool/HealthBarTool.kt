// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/HealthBarTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Paint
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.data.HealthBarComponent

object HealthBarTool {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 55f
    }

    fun create(unit: PlacedObject): HealthBarComponent {
        val label = "血量: ${unit.health}/${unit.originalHealth}"
        val textWidth = textPaint.measureText(label) + 32f
        return HealthBarComponent(
            currentHealth = unit.health.toFloat(),
            maxHealth = unit.originalHealth.toFloat(),
            tooltipText = label,
            tooltipWidth = textWidth,
            tooltipHeight = textPaint.textSize * 1.5f
        )
    }
}