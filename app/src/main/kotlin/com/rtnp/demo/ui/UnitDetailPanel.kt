package com.rtnp.demo.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.PlacedObject

/**
 * 单位详情面板：显示选中单位或物品的详细信息
 */
class UnitDetailPanel {

    private val bgPaint: Paint = Paint().apply {
        color = Color.argb(220, 64, 64, 64)
        style = Paint.Style.FILL
    }

    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.LEFT
    }

    private val hpPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 200, 200)
        textSize = 28f
        textAlign = Paint.Align.LEFT
    }

    private val costPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 255, 150)
        textSize = 28f
        textAlign = Paint.Align.LEFT
    }

    private val padding = 16
    private val lineHeight = 40
    private var panelHeight = 0

    init {
        panelHeight = (textPaint.textSize + hpPaint.textSize + costPaint.textSize + padding * 2 + 20).toInt()
    }

    fun getPanelHeight(): Int = panelHeight

    /**
     * 绘制物品详情面板（物品栏选中时使用）
     */
    fun draw(canvas: Canvas, left: Int, top: Int, maxWidth: Int, item: InventoryItem?) {
        if (item == null) return

        val nameText = item.name
        val hpText = "血量: ${item.health}"
        val costText = if (item.costItem != null && item.costItem!!.isNotEmpty() && item.costAmount > 0) {
            "消耗: ${item.costItem} x${item.costAmount}"
        } else {
            ""
        }

        val nameWidth = textPaint.measureText(nameText)
        val hpWidth = hpPaint.measureText(hpText)
        val costWidth = costPaint.measureText(costText)
        val maxTextWidth = maxOf(nameWidth, hpWidth, costWidth)
        val contentWidth = (maxTextWidth + padding * 2).toInt()
        val panelWidth = minOf(contentWidth, maxWidth)

        canvas.drawRect(left.toFloat(), top.toFloat(),
                        (left + panelWidth).toFloat(), (top + panelHeight).toFloat(), bgPaint)

        var x = left + padding
        var y = top + padding + textPaint.textSize.toInt()

        canvas.drawText(nameText, x.toFloat(), y.toFloat(), textPaint)

        y += lineHeight
        canvas.drawText(hpText, x.toFloat(), y.toFloat(), hpPaint)

        if (costText.isNotEmpty()) {
            y += lineHeight
            canvas.drawText(costText, x.toFloat(), y.toFloat(), costPaint)
        }
    }

    /**
     * 绘制单位详情面板（地图上选中单位时使用）
     * 支持显示单位的各种属性
     */
    fun drawForUnit(canvas: Canvas, left: Int, top: Int, maxWidth: Int, unit: PlacedObject?) {
        if (unit == null) return

        val nameText = unit.displayName ?: unit.name
        val lines = mutableListOf<String>()
        lines.add("血量: ${unit.health}/${unit.originalHealth}")
        lines.add("类型: ${unit.type}")
        lines.add("阵营: ${factionToString(unit.faction)}")
        if (unit.maxSpeed > 0) {
            lines.add("速度: ${unit.currentSpeed.toInt()}/${unit.maxSpeed.toInt()}")
        }
        if (unit.storageCapacity > 0) {
            val totalStored = getTotalStored(unit)
            lines.add("仓储: $totalStored/${unit.storageCapacity}")
        }

        // 计算面板高度
        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
        }
        val tempH = tempPaint.textSize + 5
        val itemHeight = (tempH * lines.size + padding * 2 + 20).toInt()
        val panelWidth = minOf(maxWidth, 200)

        canvas.drawRect(left.toFloat(), top.toFloat(),
                        (left + panelWidth).toFloat(), (top + itemHeight).toFloat(), bgPaint)

        var y = top + padding + tempPaint.textSize.toInt()
        for (line in lines) {
            canvas.drawText(line, (left + padding).toFloat(), y.toFloat(), tempPaint)
            y += tempH.toInt()
        }
    }

    private fun factionToString(faction: Int): String {
        return when (faction) {
            0 -> "中立"
            1 -> "敌对"
            2 -> "我方"
            else -> "未知"
        }
    }

    private fun getTotalStored(unit: PlacedObject): Int {
        val items = unit.collectedItems ?: return 0
        var total = 0
        // 简化版本：每个物品占1空间
        for (count in items.values) {
            total += count
        }
        return total
    }
}