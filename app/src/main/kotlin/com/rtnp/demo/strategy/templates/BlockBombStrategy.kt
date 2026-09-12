// app/src/main/kotlin/com/rtnp/demo/strategy/templates/BlockBombStrategy.kt
package com.rtnp.demo.strategy.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.render.RenderControl
import com.rtnp.demo.strategy.*
import com.rtnp.demo.ui.ToastTool
import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.logger.Logger

class BlockBombStrategy : StrategyTemplate() {
    override val id = "block_bomb"
    override val displayName = "区块炸弹"
    override val description = "向邻近区块发射集群火箭进行大规模轰炸"
    override val cooldown = 10f
    override val cooldownMode = CooldownMode.MANUAL
    override val blacklistTypes: List<String> = listOf("environment", "missile")
    override val iconPath = "images/icons/block_bomb_icon.png"

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: StrategyTools) {
        if (TileSelectorTool.isActive) {
            TileSelectorTool.updateDynamicRestriction()
        }
    }

    override fun onClicked(host: PlacedObject, tools: StrategyTools, slotIndex: Int) {
        val hostTile = HexGridManager.getTileAt(host.worldX, host.worldY)
        val hostTileId = hostTile?.id ?: -1
        Logger.i("BlockBomb", "策略激活, 宿主区块: $hostTileId")

        RenderControl.showUnitInfoPanel = false
        RenderControl.touchDisabledUnitInfoPanel = true
        RenderControl.touchDisabledBottomBar = true
        RenderControl.touchDisabledMultiSelectButton = true

        ToastTool.showGlobal("选择轰炸区") {
            cleanup()
        }
        

        TileSelectorTool.activate(tools.getScreenWidth(), tools.getScreenHeight())
        TileSelectorTool.setDynamicRangeRestriction(host, 1)

        TileSelectorTool.onTileConfirmed = { selectedTile ->
            val missile = tools.spawnUnit("集群火箭", host.worldX, host.worldY)
            if (missile != null) {
                missile.faction = host.faction
                missile.targetX = selectedTile.worldX
                missile.targetY = selectedTile.worldY
                missile.isMoving = true
                missile.currentSpeed = 0f
                missile.canMove = true

                val taskId = tools.createTask(mutableMapOf(
                    "launcherId" to host.uniqueId,
                    "missileId" to missile.uniqueId,
                    "targetTileId" to selectedTile.id,
                    "status" to "flying"
                ))
                tools.bindToTask(missile, taskId)

                tools.startCooldown(host, slotIndex, cooldown)
            }
            cleanup()
        }

        TileSelectorTool.onTileCancelled = {
            cleanup()
        }

        TileSelectorTool.onInvalidTileSelected = { _ ->
            WarningMessage.show("禁止选择此区块")
        }
    }

    private fun cleanup() {
        TileSelectorTool.stopDynamicRangeRestriction()
        TileSelectorTool.deactivate()
        RenderControl.showUnitInfoPanel = true
        RenderControl.touchDisabledUnitInfoPanel = false
        RenderControl.touchDisabledBottomBar = false
        RenderControl.touchDisabledMultiSelectButton = false
        ToastTool.hideGlobal()
    }
}