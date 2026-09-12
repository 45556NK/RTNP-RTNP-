// app/src/main/kotlin/com/rtnp/demo/strategy/templates/DeployTeleportAnchorStrategy.kt
package com.rtnp.demo.strategy.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.strategy.StrategyTemplate
import com.rtnp.demo.strategy.StrategyTools

class DeployTeleportAnchorStrategy : StrategyTemplate() {
    override val id = "deploy_teleport_anchor"
    override val displayName = "传送跃迁"
    override val description = "放置装载了区域传送的传送锚点"
    override val cooldown = 10f
    override val blacklistTypes: List<String> = listOf("environment", "missile")

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: StrategyTools) {
    }

    override fun onClicked(host: PlacedObject, tools: StrategyTools, slotIndex: Int) {
        val newUnit = tools.spawnUnit("传送锚点", host.worldX, host.worldY)
        if (newUnit != null) {
            newUnit.faction = host.faction
        }
    }
}