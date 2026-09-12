package com.rtnp.demo.trait.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.render.tools.ExplosionRangeDisplaySystem
import com.rtnp.demo.movement.ItemManager
import com.rtnp.demo.render.RenderControl
import com.rtnp.demo.trait.TraitTemplate
import com.rtnp.demo.trait.TraitTools
import com.rtnp.demo.logger.Logger
import kotlin.math.sqrt

class AntimatterBombTrait : TraitTemplate() {

    override val id = "antimatter_bomb_trait"
    override val displayName = "反物质炸弹特性"
    override val description = "根据目标矿物实时计算伤害和范围，不可自爆，爆炸后清空矿物"
    override val isGlobal = true

    // ★ 新增：记录导弹是否已真正抵达目标
    private var reachedTarget = false

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: TraitTools) {
        if (host.category != "missile") return

        if (host.isSelected) {
            RenderControl.hideStopButtonForAntimatter = true
        }

        // 初始化
        if (host.effectAngleOffset == -1f) {
            host.effectOffsetX = host.explosionRange.toFloat()
            host.effectOffsetY = host.missileDamage.toFloat()
            host.effectAngleOffset = 1f
            val target = findNearestEnvironment(host.targetX, host.targetY, host, tools)
            host.missileHealth = target?.uniqueId?.toInt() ?: -1
            reachedTarget = false  // ★ 重置
        }

        // 检测是否已抵达目标（导弹停止移动或距离极近）
        val dx = host.worldX - host.targetX
        val dy = host.worldY - host.targetY
        if (sqrt(dx * dx + dy * dy) < 2f || !host.isMoving) {
            reachedTarget = true
        }

        if (host.missileHealth == -1) return

        val baseDamage = host.effectOffsetY.toInt()
        val baseRange = host.effectOffsetX.toInt()

        val targetUnit = findUnitById(host, tools)
        if (targetUnit == null || targetUnit.type != "environment") {
            host.missileDamage = baseDamage
            host.explosionRange = baseRange
            return
        }

        if (targetUnit.storedItemName.isNullOrEmpty() || targetUnit.storedItemCount <= 0) {
            host.missileDamage = baseDamage
            host.explosionRange = baseRange
            return
        }

        val itemName = targetUnit.storedItemName
        val itemCount = targetUnit.storedItemCount
        val itemManager = ItemManager.getInstance()
        val def = itemManager.getItemDef(itemName)
        val mass = def?.mass ?: 0
        val extraDamage = itemCount * mass
        val extraRange = itemCount / 10

        if (host.effectTriggerSpeed == extraDamage.toFloat()) return

        val newDamage = baseDamage + extraDamage
        val newRange = baseRange + extraRange
        host.missileDamage = newDamage
        host.explosionRange = newRange
        host.effectTriggerSpeed = extraDamage.toFloat()
        updateExplosionRange(host, newRange)
    }

    override fun onAfterDeath(host: PlacedObject, deltaTime: Float, tools: TraitTools): Boolean {
        // ★ 只有真正抵达目标后才清除矿物
        if (!reachedTarget) return true

        val id = host.missileHealth.toLong()
        if (id > 0) {
            for ((unit, _) in tools.healthMap) {
                if (unit.uniqueId == id && unit.type == "environment") {
                    ItemManager.getInstance().clearMineral(unit)
                    return true
                }
            }
        }
        return true
    }

    override fun needsAfterDeath(): Boolean = true

    private fun updateExplosionRange(missile: PlacedObject, newRange: Int) {
        if (missile.explosionRangeDisplayId >= 0) {
            ExplosionRangeDisplaySystem.removeExplosionRange(missile.explosionRangeDisplayId)
        }
        missile.explosionRangeDisplayId = ExplosionRangeDisplaySystem.addExplosionRange(
            missile.targetX, missile.targetY, newRange.toFloat()
        )
    }

    private fun findUnitById(host: PlacedObject, tools: TraitTools): PlacedObject? {
        val id = host.missileHealth.toLong()
        if (id <= 0) return null
        for ((unit, _) in tools.healthMap) {
            if (unit.uniqueId == id) return unit
        }
        return null
    }

    private fun findNearestEnvironment(wx: Float, wy: Float, self: PlacedObject, tools: TraitTools): PlacedObject? {
        var nearest: PlacedObject? = null
        var nearestDist = Float.MAX_VALUE
        for ((unit, _) in tools.healthMap) {
            if (unit == self) continue
            if (unit.type != "environment") continue
            val dx = wx - unit.worldX
            val dy = wy - unit.worldY
            val dist = sqrt(dx * dx + dy * dy).toFloat()
            if (dist <= 100f && dist < nearestDist) {
                nearestDist = dist
                nearest = unit
            }
        }
        return nearest
    }
}