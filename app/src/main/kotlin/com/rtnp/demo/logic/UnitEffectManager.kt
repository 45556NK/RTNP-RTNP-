package com.rtnp.demo.logic

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.gpu.effect.GpuEffectSystem

object UnitEffectManager {

    private val unitEffects = mutableMapOf<PlacedObject, EffectData>()

    data class EffectData(
        var effectId: String = "",
        var offsetX: Float = 0f,
        var offsetY: Float = 0f,
        var angleOffset: Float = 180f,
        var triggerSpeed: Float = 20f,
        var activeInstance: Long = -1L
    )

    fun registerFromTemplate(unit: PlacedObject, template: InventoryItem?) {
        if (template == null || template.effectId.isEmpty()) return
        unitEffects[unit] = EffectData(
            effectId = template.effectId,
            offsetX = template.effectOffsetX,
            offsetY = template.effectOffsetY,
            angleOffset = template.effectAngleOffset,
            triggerSpeed = template.effectTriggerSpeed
        )
    }

    fun getEffectData(unit: PlacedObject): EffectData? = unitEffects[unit]
    fun unregister(unit: PlacedObject) { unitEffects.remove(unit) }

    fun updateEffects(unit: PlacedObject) {
        val data = unitEffects[unit] ?: return
        if (data.effectId.isEmpty()) return

        if (unit.currentSpeed > 0f) {
            if (data.activeInstance == -1L) {
                // 初始生成时的世界位置（因尚未跟随，任意值均可，反正会立即更新）
                GpuEffectSystem.spawn(data.effectId, unit.worldX, unit.worldY, unit, mapOf(
                    "offsetX" to data.offsetX,
                    "offsetY" to data.offsetY,
                    "angleOffset" to data.angleOffset
                ))
                data.activeInstance = 1L
            }
        } else {
            data.activeInstance = -1L
        }
    }
}