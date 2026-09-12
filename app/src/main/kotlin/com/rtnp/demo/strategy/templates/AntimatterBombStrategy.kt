package com.rtnp.demo.strategy.templates

import android.graphics.Color

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.strategy.StrategyTemplate
import com.rtnp.demo.strategy.StrategyTools
import com.rtnp.demo.strategy.StrategyToolLayer
import com.rtnp.demo.strategy.CooldownMode
import com.rtnp.demo.render.RenderControl
import com.rtnp.demo.camera.CameraAnimator
import com.rtnp.demo.ui.ToastTool
import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.ui.GameButton

class AntimatterBombStrategy : StrategyTemplate() {
    override val id = "antimatter_bomb"
    override val displayName = "反物质炸弹"
    override val description = "发射一枚毁天灭地的反物质导弹"
    override val cooldown = 10f
    override val cooldownMode = CooldownMode.MANUAL
    override val blacklistTypes: List<String> = listOf("base", "missile")

    companion object {
        const val MAX_RANGE = 1000f
        const val MISSILE_TYPE = "反物质导弹"
        const val BOMB_TRAIT_ID = "antimatter_bomb_trait"
    }

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: StrategyTools) {
    }

    override fun onClicked(host: PlacedObject, tools: StrategyTools, slotIndex: Int) {
        
        CameraAnimator.animateZoomAndMove(host.worldX, host.worldY, 0.8f, 800L)

        StrategyToolLayer.activate(host, MAX_RANGE)

        RenderControl.touchDisabledUnitInfoPanel = true
        RenderControl.touchDisabledBottomBar = true
        RenderControl.touchDisabledMultiSelectButton = true
        RenderControl.showUnitInfoPanel = false

        ToastTool.showGlobal("请选择目标发射导弹") {
            cleanup()
        }

        StrategyToolLayer.onTapInside = { wx, wy ->
            val targetUnit = findUnitAt(wx, wy, host, tools)
            if (targetUnit != null) {
                val missile = tools.launchMissileAtCoord(host, targetUnit.worldX, targetUnit.worldY, MISSILE_TYPE)
                missile?.traitIds?.add(BOMB_TRAIT_ID)
                tools.startCooldown(host, slotIndex, cooldown)
                cleanup()
            } else {
                ToastTool.showGlobal("未命中目标，请重新选择") {
                    cleanup()
                }
            }
        }
        StrategyToolLayer.onTapOutside = {
            WarningMessage.show("超出射程！")
        }
    }

    private fun findUnitAt(wx: Float, wy: Float, host: PlacedObject, tools: StrategyTools): PlacedObject? {
        for (unit in tools.getAllUnits()) {
            if (unit == host) continue
            if (unit.faction == host.faction) continue
            if (unit.category == "missile") continue

            val hp = tools.getHealth(unit)
            if (hp <= 0) continue

            if (unit.shape == "circle") {
                val r = unit.size / 2f + 20f
                val dx = wx - unit.worldX
                val dy = wy - unit.worldY
                if (dx * dx + dy * dy <= r * r) return unit
            } else {
                val halfW = unit.width / 2f + 20f
                val halfH = unit.height / 2f + 20f
                if (Math.abs(wx - unit.worldX) <= halfW && Math.abs(wy - unit.worldY) <= halfH) return unit
            }
        }
        return null
    }

    private fun cleanup() {
        StrategyToolLayer.deactivate()
        RenderControl.touchDisabledUnitInfoPanel = false
        RenderControl.touchDisabledBottomBar = false
        RenderControl.touchDisabledMultiSelectButton = false
        RenderControl.showUnitInfoPanel = true
        ToastTool.hideGlobal()
    }
}