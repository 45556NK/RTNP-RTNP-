// app/src/main/kotlin/com/rtnp/demo/strategy/templates/ClusterBombStrategy.kt
package com.rtnp.demo.strategy.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.strategy.StrategyTemplate
import com.rtnp.demo.strategy.StrategyTools
import kotlin.math.sqrt

class ClusterBombStrategy : StrategyTemplate() {
    override val id = "cluster_bomb"
    override val displayName = "集群炸弹"
    override val description = "到达目标区块后向所有环境单位发射\n集群导弹，然后自毁"
    override val cooldown = 0f
    override val hiddenFromPlayer = true
    override val blacklistTypes: List<String> = listOf("environment", "missile")

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: StrategyTools) {
        // effectOffsetX: 0=未完成, 1=已执行
        if (host.effectOffsetX == 1f) return

        // 1. 获取任务数据
        val task = tools.getTaskData(host) ?: return
        val status = task.getString("status") ?: return
        if (status != "flying") return

        val targetTileId = task.getInt("targetTileId") ?: return

        // 2. 检测宿主是否到达目标区块
        val currentTile = HexGridManager.getTileAt(host.worldX, host.worldY)
        if (currentTile == null || currentTile.id != targetTileId) return

        // 3. 已到达，执行轰炸
        host.effectOffsetX = 1f

        // 获取目标区块内所有环境单位
        val unitsInTile = HexGridManager.getUnitsInTile(targetTileId)
        val envUnits = mutableListOf<PlacedObject>()
        for (unit in unitsInTile) {
            if (unit.type == "environment") {
                envUnits.add(unit)
            }
        }

        if (envUnits.isEmpty()) {
            tools.completeTask(task.taskId)
            tools.removeUnit(host)
            return
        }

        // 4. 为每个环境单位创建一枚集群导弹
        for (target in envUnits) {
            tools.launchMissileAtCoord(
                launcher = host,
                targetWorldX = target.worldX,
                targetWorldY = target.worldY,
                missileTypeName = "集群导弹"
            )
        }

        // 5. 完成任务并自毁
        tools.completeTask(task.taskId)
        tools.removeUnit(host)
    }

    override fun onClicked(host: PlacedObject, tools: StrategyTools, slotIndex: Int) {
    }
}