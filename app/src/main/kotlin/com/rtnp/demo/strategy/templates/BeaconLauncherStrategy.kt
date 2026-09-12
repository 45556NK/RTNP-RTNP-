// app/src/main/kotlin/com/rtnp/demo/strategy/templates/BeaconLauncherStrategy.kt
package com.rtnp.demo.strategy.templates

import com.rtnp.demo.camera.CameraAnimator
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.render.RenderControl
import com.rtnp.demo.strategy.*
import com.rtnp.demo.ui.ToastTool
import com.rtnp.demo.ui.WarningMessage

class BeaconLauncherStrategy : StrategyTemplate() {
    override val id = "beacon_launcher"
    override val displayName = "灯塔发射器"
    override val description = "向1000像素内指定坐标发射B1运载火箭\n部署漂泊信标"
    override val cooldown = 10f
    override val cooldownMode = CooldownMode.MANUAL
    override val blacklistTypes: List<String> = listOf("environment", "missile")

    companion object {
        const val MAX_RANGE = 1000f
    }

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: StrategyTools) {
    }

    override fun onClicked(host: PlacedObject, tools: StrategyTools, slotIndex: Int) {
        // 视角拉回到宿主，缩放0.8
        CameraAnimator.animateZoomAndMove(host.worldX, host.worldY, 0.8f, 800L)

        // 隐藏UI，停止触摸
        RenderControl.showUnitInfoPanel = false
        RenderControl.touchDisabledUnitInfoPanel = true
        RenderControl.touchDisabledBottomBar = true
        RenderControl.touchDisabledMultiSelectButton = true

        // 显示提示栏
        ToastTool.showGlobal("请选择目的地") {
            cleanup()
        }

        // 激活范围限制工具
        StrategyToolLayer.activate(host, MAX_RANGE)

        StrategyToolLayer.onTapInside = { wx, wy ->
            val rocket = tools.spawnUnit("B1运载火箭", host.worldX, host.worldY)
            if (rocket != null) {
                rocket.faction = host.faction

                val taskId = tools.createTask(mutableMapOf(
                    "status" to "flying",
                    "targetX" to wx,
                    "targetY" to wy,
                    "spawnUnitName" to "漂泊信标"
                ))
                tools.bindToTask(rocket, taskId)

                tools.startCooldown(host, slotIndex, cooldown)
            }
            cleanup()
        }

        StrategyToolLayer.onTapOutside = {
            WarningMessage.show("超越射程！")
        }
    }

    private fun cleanup() {
        StrategyToolLayer.deactivate()
        RenderControl.showUnitInfoPanel = true
        RenderControl.touchDisabledUnitInfoPanel = false
        RenderControl.touchDisabledBottomBar = false
        RenderControl.touchDisabledMultiSelectButton = false
        ToastTool.hideGlobal()
    }
}