// app/src/main/kotlin/com/rtnp/demo/strategy/templates/RegenerationEngineeringStrategy.kt
package com.rtnp.demo.strategy.templates

import android.graphics.Color
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.strategy.CooldownMode
import com.rtnp.demo.strategy.StrategyTemplate
import com.rtnp.demo.strategy.StrategyTools
import com.rtnp.demo.ui.panel.tool.EnergyRingRenderTool
import com.rtnp.demo.ui.panel.tool.StrategyButtonTool

class RegenerationEngineeringStrategy : StrategyTemplate() {

    override val id = "regeneration_engineering"
    override val displayName = "再生工程"
    override val description = "小型规模化的组装工厂会收集战场中的残骸\n以快速恢复船体的结构"
    override val iconPath = "images/icons/regeneration_engineering_icon.png"
    override val cooldown = 0f
    override val cooldownMode = CooldownMode.MANUAL
    override val needsRender = false

    companion object {
        const val THRESHOLD = 1500f
        const val REPAIR_RATIO = 0.2f
        const val ABSORB_RATIO = 0.1f
    }

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: StrategyTools) {
        val slotIndex = host.strategyIds.indexOf(id)
        if (slotIndex < 0) return

        var accumulated = host.effectOffsetX
        val hostTileId = HexGridManager.getTileIdForUnit(host) ?: return

        // ★ 使用 getRecentlyDeadUnits 获取本帧死亡单位
        for (deadUnit in tools.getRecentlyDeadUnits()) {
            if (deadUnit.type == "environment" || deadUnit.category == "missile") continue
            val deadTileId = HexGridManager.getTileIdForUnit(deadUnit) ?: continue
            if (deadTileId in getNearbyTileIds(hostTileId)) {
                accumulated += deadUnit.originalHealth * ABSORB_RATIO
            }
        }

        if (accumulated >= THRESHOLD) {
            val currentHp = tools.getHealth(host)
            val maxHp = host.originalHealth.toFloat()
            if (currentHp / maxHp < 1f - REPAIR_RATIO) {
                tools.setHealth(host, (currentHp + maxHp * REPAIR_RATIO).coerceAtMost(maxHp))
                accumulated = 0f
            }
        }

        host.effectOffsetX = accumulated
        val progress = (accumulated / THRESHOLD).coerceIn(0f, 1f)
        StrategyButtonTool.setEnergy(host, slotIndex, progress, Color.rgb(29, 99, 0), EnergyRingRenderTool.Direction.CLOCKWISE)
    }

    private fun getNearbyTileIds(centerTileId: Int): Set<Int> {
        val tile = HexGridManager.getTileById(centerTileId) ?: return emptySet()
        val ids = mutableSetOf(centerTileId)
        for (dq in -1..1) {
            for (dr in -1..1) {
                HexGridManager.getTileByAxial(tile.q + dq, tile.r + dr)?.let { ids.add(it.id) }
            }
        }
        return ids
    }

    override fun onClicked(host: PlacedObject, tools: StrategyTools, slotIndex: Int) {}
}