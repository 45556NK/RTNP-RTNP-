// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/StrategyButtonTool.kt
package com.rtnp.demo.ui.panel.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.strategy.StrategyManager
import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent

object StrategyButtonTool {

    private const val BORDER_IMAGE = "images/strategy_border.svg"

    private val energyDataMap = mutableMapOf<Long, MutableMap<Int, EnergyData>>()

    data class EnergyData(
        var progress: Float = 0f,
        var color: Int = Color.argb(200, 100, 200, 255),
        var direction: EnergyRingRenderTool.Direction = EnergyRingRenderTool.Direction.CLOCKWISE
    )

    fun create(strategyId: String, index: Int, vw: Int, context: Context, unit: PlacedObject): InteractiveButtonComponent {
        val size = InteractiveButtonTool.calculateSize(vw)
        val borderBitmap = ImageManager.getInstance(context).getBitmap(BORDER_IMAGE)
        val iconBitmap = loadStrategyIcon(strategyId, context)

        val label = if (iconBitmap != null) "" else buildLabel(strategyId, index)

        val animLabel = if (strategyId.isNotEmpty()) {
            StrategyManager.getTemplate(strategyId)?.displayName ?: ""
        } else ""

        val btn = InteractiveButtonComponent(
            label = label,
            width = size,
            height = size,
            backgroundColor = Color.argb(25, 72, 0, 72),
            borderBitmap = borderBitmap,
            iconBitmap = iconBitmap,
            animLabel = animLabel,
            onClick = {
                if (strategyId.isEmpty()) return@InteractiveButtonComponent
                val cooldownBar = if (index < unit.strategyCooldownBars.size) unit.strategyCooldownBars[index] else null
                if (cooldownBar != null && !cooldownBar.isFinished) {
                    WarningMessage.show("冷却中")
                    return@InteractiveButtonComponent
                }
                StrategyManager.onClickStrategy(unit, strategyId, index)
            },
            onPressed = { i -> ButtonAnimTool.defaultOnPress(i) },
            onReleased = { i -> ButtonAnimTool.defaultOnRelease(i) },
            onCancelled = { i -> ButtonAnimTool.defaultOnCancel(i) }
        )
        btn.customData = StrategyButtonData(index)
        return btn
    }

    private fun loadStrategyIcon(strategyId: String, context: Context): Bitmap? {
        if (strategyId.isEmpty()) return null
        val template = StrategyManager.getTemplate(strategyId) ?: return null
        val iconPath = template.iconPath ?: return null
        return ImageManager.getInstance(context).getBitmap(iconPath)
    }

    private fun buildLabel(strategyId: String, index: Int): String {
        if (strategyId.isEmpty()) return "策略 ${index + 1}"
        return StrategyManager.getTemplate(strategyId)?.displayName ?: strategyId
    }

    /**
     * ★ 每帧同步策略冷却进度
     */
    fun updateCooldowns(buttons: List<InteractiveButtonComponent>, unit: PlacedObject) {
        for (btn in buttons) {
            val data = btn.customData as? StrategyButtonData ?: continue
            val slotIndex = data.slotIndex
            if (slotIndex < 0 || slotIndex >= unit.strategyCooldownBars.size) continue
            val bar = unit.strategyCooldownBars[slotIndex]
            if (bar != null && !bar.isFinished) {
                btn.cooldownProgress = bar.progress
                btn.cooldownColor = Color.argb(89, 255, 130, 130)
                btn.cooldownDirection = CooldownRenderTool.Direction.BOTTOM_UP
            } else {
                btn.cooldownProgress = 0f
            }
        }
    }

    /**
     * ★ 外部策略调用，设置能量环显示
     */
    fun setEnergy(unit: PlacedObject, slotIndex: Int, progress: Float, color: Int, direction: EnergyRingRenderTool.Direction) {
        val unitData = energyDataMap.getOrPut(unit.uniqueId) { mutableMapOf() }
        unitData[slotIndex] = EnergyData(progress, color, direction)
    }

    /**
     * ★ 每帧将能量数据同步到按钮
     */
    fun updateEnergy(buttons: List<InteractiveButtonComponent>, unit: PlacedObject) {
        val unitData = energyDataMap[unit.uniqueId] ?: return
        for (btn in buttons) {
            val data = btn.customData as? StrategyButtonData ?: continue
            val slotData = unitData[data.slotIndex] ?: continue
            btn.energyProgress = slotData.progress
            btn.energyColor = slotData.color
            btn.energyDirection = slotData.direction
        }
    }
}

data class StrategyButtonData(
    val slotIndex: Int
)