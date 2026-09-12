package com.rtnp.demo.strategy.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.render.tools.StrategyRenderSystem
import com.rtnp.demo.strategy.StrategyTemplate
import com.rtnp.demo.strategy.StrategyTools
import kotlin.math.sqrt

class RepairBeamStrategy : StrategyTemplate() {
    override val id = "repair_beam"
    override val displayName = "热重组"
    override val description = "消耗金属矿物，每秒为200像素内最近的\n个受损友军修复一定的血量"
    override val cooldown = 0f
    override val blacklistTypes: List<String> = listOf("environment", "missile")
    override val needsGpuRender = true

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: StrategyTools) {
        val items = host.collectedItems
        val metalCount = items?.get("金属") ?: 0

        if (items == null || metalCount <= 0) {
            host.effectOffsetX = 0f
            return
        }

        val target = findNearestDamagedAlly(host, tools)
        if (target == null) {
            host.effectOffsetX = 0f
            return
        }

        host.effectOffsetX = target.uniqueId.toFloat()

        host.effectTriggerSpeed += deltaTime
        if (host.effectTriggerSpeed >= 1f) {
            host.effectTriggerSpeed -= 1f

            val currentMetal = items["金属"] ?: 0
            if (currentMetal <= 0) {
                host.effectOffsetX = 0f
                return
            }
            items["金属"] = currentMetal - 1

            val currentHp = tools.getHealth(target)
            val newHp = (currentHp + 80f).coerceAtMost(target.originalHealth.toFloat())
            tools.setHealth(target, newHp)
        }
    }

    override fun onGpuRender(host: PlacedObject, tools: StrategyTools, renderSystem: StrategyRenderSystem) {
        val items = host.collectedItems
        val metalCount = items?.get("金属") ?: 0

        // 范围圈仅在金属足够时绘制
        if (metalCount > 0) {
            renderSystem.addCircle(
                host.worldX, host.worldY, 200f,
                floatArrayOf(144f / 255f, 238f / 255f, 144f / 255f, 46f / 255f)
            )
            renderSystem.addRing(
                host.worldX, host.worldY, 200f, 199f,
                floatArrayOf(0f, 100f / 255f, 0f, 128f / 255f)
            )
        }

        // ★ 矿物不足时不渲染修复射线
        if (metalCount <= 0) return

        val target = findNearestDamagedAlly(host, tools) ?: return

        val lineColor = floatArrayOf(0f, 1f, 100f / 255f, 1f)
        renderSystem.addLine(host.worldX, host.worldY, target.worldX, target.worldY, lineColor, 10f)
        renderSystem.addCircle(host.worldX, host.worldY, 10f, lineColor)
        renderSystem.addCircle(target.worldX, target.worldY, 10f, lineColor)
    }

    private fun findNearestDamagedAlly(host: PlacedObject, tools: StrategyTools): PlacedObject? {
        var nearest: PlacedObject? = null
        var nearestDist = Float.MAX_VALUE

        for (unit in tools.getAllUnits()) {
            if (unit == host) continue
            if (unit.faction != host.faction) continue
            if (unit.health >= unit.originalHealth) continue
            // ★ 过滤建造中单位
            if (unit.category == "建造中单位") continue

            val dx = host.worldX - unit.worldX
            val dy = host.worldY - unit.worldY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist <= 200f && dist < nearestDist) {
                nearestDist = dist
                nearest = unit
            }
        }
        return nearest
    }
}