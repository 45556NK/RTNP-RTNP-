package com.rtnp.demo.ui.panel.tool

import android.graphics.Paint
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.BuildManager
import com.rtnp.demo.ui.panel.data.BuildReserveComponent

object BuildReserveTool {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 55f
    }

    fun create(unit: PlacedObject): BuildReserveComponent? {
        if (unit.category != "建造中单位") return null

        val process = BuildManager.getBuildProcesses().firstOrNull { it.invisibleUnit == unit }
            ?: return null

        val costItem = process.item.costItem
        val costAmount = process.item.costAmount
        if (costItem.isNullOrEmpty() || costAmount <= 0) return null

        val submitted = process.submittedMaterials
        val percent = if (costAmount > 0) (submitted.toFloat() / costAmount * 100).toInt() else 0

        val label = "建造储备:$percent%"
        val lines = mutableListOf(label)
        lines.add("$costItem: $submitted/$costAmount")

        val maxWidth = lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f
        val lineHeight = textPaint.textSize * 1.5f

        return BuildReserveComponent(
            currentReserve = submitted,
            maxReserve = costAmount,
            tooltipText = label,
            tooltipLines = lines,
            tooltipWidth = maxWidth + 32f,
            tooltipHeight = lineHeight * lines.size + 16f
        )
    }
}