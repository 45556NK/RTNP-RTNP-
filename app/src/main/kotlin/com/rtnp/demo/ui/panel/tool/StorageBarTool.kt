// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/StorageBarTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Paint
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.movement.ItemManager
import com.rtnp.demo.ui.panel.data.StorageBarComponent

object StorageBarTool {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 55f
    }

    fun create(unit: PlacedObject): StorageBarComponent? {
        val itemMgr = ItemManager.getInstance()

        if (unit.type == "environment") {
            if (unit.storedItemCount <= 0 && unit.storageCapacity <= 0) return null
            val itemName = unit.storedItemName ?: ""
            val def = itemMgr.getItemDef(itemName)
            val space = def?.space ?: 1
            val occupied = unit.storedItemCount * space
            val maxStorage = if (unit.storageCapacity > 0) unit.storageCapacity else occupied
            val label = "资源: $occupied/$maxStorage"
            val lines = mutableListOf(label)
            if (itemName.isNotEmpty()) {
                lines.add("$itemName: ${unit.storedItemCount}")
            }
            val maxWidth = lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f
            val lineHeight = textPaint.textSize * 1.5f
            return StorageBarComponent(
                currentStorage = occupied,
                maxStorage = maxStorage,
                tooltipText = label,
                tooltipLines = lines,
                tooltipWidth = maxWidth + 32f,
                tooltipHeight = lineHeight * lines.size + 16f
            )
        }

        if (unit.storageCapacity <= 0) return null

        var totalOccupied = 0
        val lines = mutableListOf<String>()
        unit.collectedItems?.forEach { (name, count) ->
            val def = itemMgr.getItemDef(name)
            val space = def?.space ?: 1
            val occupied = count * space
            totalOccupied += occupied
            if (count > 0) lines.add("$name: $count")
        }

        val label = "仓储: $totalOccupied/${unit.storageCapacity}"
        lines.add(0, label)

        val maxWidth = lines.maxOfOrNull { textPaint.measureText(it) } ?: 0f
        val lineHeight = textPaint.textSize * 1.5f
        return StorageBarComponent(
            currentStorage = totalOccupied,
            maxStorage = unit.storageCapacity,
            tooltipText = label,
            tooltipLines = lines,
            tooltipWidth = maxWidth + 32f,
            tooltipHeight = lineHeight * lines.size + 16f
        )
    }
}