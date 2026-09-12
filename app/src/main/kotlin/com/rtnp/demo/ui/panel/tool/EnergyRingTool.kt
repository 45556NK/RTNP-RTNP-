// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/EnergyRingTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Color
import com.rtnp.demo.core.PlacedObject

object EnergyRingTool {

    private val energyData = mutableMapOf<PlacedObject.WeaponSlot, EnergyData>()

    data class EnergyData(
        var totalDuration: Float = 1f,
        var elapsed: Float = 0f,
        var color: Int = Color.argb(200, 100, 200, 255),
        var direction: EnergyRingRenderTool.Direction = EnergyRingRenderTool.Direction.CLOCKWISE,
        var active: Boolean = false
    )

    /**
     * ★ 模式1：工具自己计时
     */
    fun start(slot: PlacedObject.WeaponSlot, duration: Float, color: Int, clockwise: Boolean) {
        val data = energyData.getOrPut(slot) { EnergyData() }
        data.totalDuration = duration
        data.elapsed = 0f
        data.color = color
        data.direction = if (clockwise) EnergyRingRenderTool.Direction.CLOCKWISE else EnergyRingRenderTool.Direction.COUNTERCLOCKWISE
        data.active = true
    }

    /**
     * ★ 模式2：直接从武器槽读取充能进度，不自己计时
     */
    fun startFromSlot(slot: PlacedObject.WeaponSlot, color: Int, clockwise: Boolean) {
        val data = energyData.getOrPut(slot) { EnergyData() }
        data.color = color
        data.direction = if (clockwise) EnergyRingRenderTool.Direction.CLOCKWISE else EnergyRingRenderTool.Direction.COUNTERCLOCKWISE
        data.active = true
        data.totalDuration = -1f  // ★ 标记为模式2
        data.elapsed = 0f
    }

    fun stop(slot: PlacedObject.WeaponSlot) {
        energyData[slot]?.active = false
    }

    /**
     * ★ 模式1的每帧更新
     */
    fun update(deltaTime: Float) {
        for ((_, data) in energyData) {
            if (!data.active) continue
            if (data.totalDuration <= 0f) continue
            data.elapsed += deltaTime
            if (data.elapsed >= data.totalDuration) {
                data.elapsed = data.totalDuration
            }
        }
    }

    /**
     * ★ 将能量数据同步到按钮组件上
     * @param progressOverride 模式2下外部传入的进度值
     */
    fun applyToButton(slot: PlacedObject.WeaponSlot, btn: com.rtnp.demo.ui.panel.data.InteractiveButtonComponent, progressOverride: Float? = null) {
        val data = energyData[slot] ?: return
        if (!data.active) {
            btn.energyProgress = 0f
            return
        }
        val progress = when {
            data.totalDuration > 0f -> (data.elapsed / data.totalDuration).coerceIn(0f, 1f)
            progressOverride != null -> progressOverride.coerceIn(0f, 1f)
            else -> 0f
        }
        btn.energyProgress = progress
        btn.energyColor = data.color
        btn.energyDirection = data.direction
    }
}