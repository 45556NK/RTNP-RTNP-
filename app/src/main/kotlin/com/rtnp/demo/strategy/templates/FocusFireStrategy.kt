package com.rtnp.demo.strategy.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.strategy.StrategyTemplate
import com.rtnp.demo.strategy.StrategyTools

class FocusFireStrategy : StrategyTemplate() {
    override val id = "focus_fire"
    override val displayName = "脉冲塔"
    override val description = "部署一个小型防御性建筑\n寿命较短"
    override val cooldown = 10f
    override val blacklistTypes: List<String> = listOf("base", "missile")

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: StrategyTools) {
    }

    override fun onClicked(host: PlacedObject, tools: StrategyTools, slotIndex: Int) {
        val spawnX = host.worldX
        val spawnY = host.worldY

        val newUnit = tools.spawnUnit("脉冲塔", spawnX, spawnY)
        if (newUnit != null) {
            newUnit.faction = host.faction
        }
    }
}