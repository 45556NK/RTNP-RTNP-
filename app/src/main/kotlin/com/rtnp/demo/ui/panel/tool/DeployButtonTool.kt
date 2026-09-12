package com.rtnp.demo.ui.panel.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.ai.AiManager
import com.rtnp.demo.ai.automation.EngineerAutoDeployAI
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.logic.DockingSystem
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.render.RenderControl
import com.rtnp.demo.strategy.TileSelectorTool
import com.rtnp.demo.ui.ToastTool
import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.ui.panel.RightPanel
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent
import kotlin.random.Random

object DeployButtonTool {

    private const val ICON_PATH = "images/ui/deploy_btn.png"
    private const val OFF_OVERLAY_PATH = "images/ui/deploy_off_overlay.png"
    private const val AUTO_DEPLOY_AI_ID = "engineer_auto_deploy"
    private const val AI_ADD_DELAY_MS = 300L

    // 每个工程船独立状态
    private val deployStates = mutableMapOf<Long, DeployButtonData>()

    data class DeployButtonData(
        var isDeployMode: Boolean = false,
        val offOverlay: Bitmap? = null
    )

    fun create(vw: Int, context: Context, unit: PlacedObject): InteractiveButtonComponent {
        val size = InteractiveButtonTool.calculateSize(vw)
        val offOverlay = ImageManager.getInstance(context).getBitmap(OFF_OVERLAY_PATH)
        val data = deployStates.getOrPut(unit.uniqueId) { DeployButtonData(offOverlay = offOverlay) }

        val btn = InteractiveButtonComponent(
            label = "",
            width = size,
            height = size,
            backgroundColor = Color.argb(25, 0, 0, 0),
            iconBitmap = ImageManager.getInstance(context).getBitmap(ICON_PATH),
            animLabel = "工程部署",
            onClick = { toggleDeployMode(unit, data) },
            onPressed = { i -> ButtonAnimTool.defaultOnPress(i) },
            onReleased = { i -> ButtonAnimTool.defaultOnRelease(i) },
            onCancelled = { i -> ButtonAnimTool.defaultOnCancel(i) }
        )
        btn.customData = data
        return btn
    }

    fun getAlpha(comp: InteractiveButtonComponent): Pair<Int, Int> {
        val data = comp.customData as? DeployButtonData
        return if (data?.isDeployMode == true) Pair(255, 255) else Pair(128, 128)
    }

    fun getOverlayAlpha(comp: InteractiveButtonComponent): Int {
        val data = comp.customData as? DeployButtonData
        return if (data?.isDeployMode == true) 0 else 255
    }

    fun getOverlay(comp: InteractiveButtonComponent): Bitmap? {
        val data = comp.customData as? DeployButtonData
        return data?.offOverlay
    }

    private fun toggleDeployMode(engineer: PlacedObject, data: DeployButtonData) {
        if (data.isDeployMode) {
            cancelDeployFlow(engineer, data)
        } else {
            val us = RightPanel.getUnitSystem() ?: return
            if (engineer.isMoving || engineer.isStopping || engineer.isUndocking) {
                WarningMessage.show("当前无法部署")
                return
            }
            startDeployFlow(engineer, data)
        }
    }

    private fun startDeployFlow(engineer: PlacedObject, data: DeployButtonData) {
        val us = RightPanel.getUnitSystem() ?: return
        data.isDeployMode = true

        RenderControl.showUnitInfoPanel = false
        RenderControl.touchDisabledUnitInfoPanel = true
        RenderControl.touchDisabledBottomBar = true

        ToastTool.showGlobal("请选择一个合适的区块部署") {
            cancelDeployFlow(engineer, data)
        }
        ToastTool.globalInstance?.setOffset(0f, -300f)

        TileSelectorTool.activate(us.screenWidth, us.screenHeight)
        TileSelectorTool.onTileConfirmed = { tile ->
            val target = findDeployTarget(engineer, tile.id, us)
            if (target != null) {
                DockingSystem.requestDock(engineer, target)
                cleanupUiOnly()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    EngineerAutoDeployAI.clearState(engineer)
                    AiManager.addAi(engineer, AUTO_DEPLOY_AI_ID)
                }, AI_ADD_DELAY_MS)
            } else {
                WarningMessage.show("该区块无可用部署目标")
                cancelDeployFlow(engineer, data)
            }
        }
        TileSelectorTool.onTileCancelled = {
            cancelDeployFlow(engineer, data)
        }
    }

    private fun cleanupUiOnly() {
        TileSelectorTool.deactivate()
        RenderControl.showUnitInfoPanel = true
        RenderControl.touchDisabledUnitInfoPanel = false
        RenderControl.touchDisabledBottomBar = false
        ToastTool.hideGlobal()
    }

    private fun cancelDeployFlow(engineer: PlacedObject, data: DeployButtonData) {
        cleanupUiOnly()
        AiManager.removeAi(engineer, AUTO_DEPLOY_AI_ID)
        EngineerAutoDeployAI.clearState(engineer)
        data.isDeployMode = false
    }

    private fun findDeployTarget(engineer: PlacedObject, tileId: Int, us: UnitSystem): PlacedObject? {
        val unitsInTile = HexGridManager.getUnitsInTile(tileId)

        for (obj in unitsInTile) {
            if (obj.category != "建造中单位") continue
            if (obj.faction != engineer.faction) continue
            val slots = DockingSystem.getSlotCount(obj)
            val occupied = obj.dockedList.size + obj.dockingList.size
            if (occupied < slots) return obj
        }

        for (obj in unitsInTile) {
            if (obj === engineer) continue
            if (obj.faction != engineer.faction) continue
            if (obj.type != "base") continue
            val slots = DockingSystem.getSlotCount(obj)
            if (slots > 0) return obj
        }

        val env = unitsInTile.filter { obj ->
            obj !== engineer &&
            obj.type == "environment" &&
            DockingSystem.getSlotCount(obj) > 0
        }
        if (env.isNotEmpty()) {
            return env[Random.nextInt(env.size)]
        }

        return null
    }

    fun resetAll() {
        val us = RightPanel.getUnitSystem() ?: return
        for (unit in us.placedObjects) {
            AiManager.removeAi(unit, AUTO_DEPLOY_AI_ID)
            EngineerAutoDeployAI.clearState(unit)
        }
        deployStates.clear()
    }
}