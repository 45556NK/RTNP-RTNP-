package com.rtnp.demo.ui.panel.tool

import android.graphics.Paint
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.data.SpeedBarComponent

object SpeedBarTool {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 55f
    }

    fun create(unit: PlacedObject): SpeedBarComponent {
        val current: Float
        val max: Float
        if (unit.category == "missile") {
            current = unit.missileCurrentSpeed
            max = unit.missileMaxSpeed.toFloat()
        } else {
            current = unit.currentSpeed
            max = unit.maxSpeed
        }
        val label = "速度: ${current.toInt()}/${max.toInt()}"
        val textWidth = textPaint.measureText(label) + 32f
        return SpeedBarComponent(
            currentSpeed = current,
            maxSpeed = max,
            tooltipText = label,
            tooltipWidth = textWidth,
            tooltipHeight = textPaint.textSize * 1.5f
        )
    }
}