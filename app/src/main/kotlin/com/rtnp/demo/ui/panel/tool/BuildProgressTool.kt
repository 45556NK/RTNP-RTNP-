package com.rtnp.demo.ui.panel.tool

import android.graphics.Paint
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.BuildManager
import com.rtnp.demo.ui.panel.data.BuildProgressComponent

object BuildProgressTool {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 55f
    }

    fun create(unit: PlacedObject): BuildProgressComponent? {
        if (unit.category != "建造中单位") return null
        val process = BuildManager.getBuildProcesses().firstOrNull { it.invisibleUnit == unit } ?: return null
        val current = process.progress
        val max = process.item.buildAmount
        val label = "建造进度: ${current.toInt()}/${max.toInt()}"
        val textWidth = textPaint.measureText(label) + 32f
        return BuildProgressComponent(
            currentBuild = current,
            maxBuild = max,
            tooltipText = label,
            tooltipLines = listOf(label),
            tooltipWidth = textWidth,
            tooltipHeight = textPaint.textSize * 1.5f
        )
    }
}