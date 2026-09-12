// app/src/main/kotlin/com/rtnp/demo/strategy/templates/SpawnRepairShipStrategy.kt
package com.rtnp.demo.strategy.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.strategy.StrategyTemplate
import com.rtnp.demo.strategy.StrategyTools

class SpawnRepairShipStrategy : StrategyTemplate() {
    override val id = "spawn_repair_ship"
    override val displayName = "尘埃组装\n无人机"
    override val description = "在当前位置部署一艘修复无人机"
    override val cooldown = 10f
    override val blacklistTypes: List<String> = listOf("environment", "missile")

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: StrategyTools) {
    }

    override fun onClicked(host: PlacedObject, tools: StrategyTools, slotIndex: Int) {
        val newUnit = tools.spawnUnit("尘埃重组无人机", host.worldX, host.worldY)
        if (newUnit != null) {
            newUnit.faction = host.faction
        }
    }
}