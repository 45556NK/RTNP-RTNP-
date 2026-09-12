// app/src/main/kotlin/com/rtnp/demo/ui/action/ActionFlowController.kt
package com.rtnp.demo.ui.action

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.DockingSystem
import com.rtnp.demo.render.RenderControl
import com.rtnp.demo.ui.ToastTool
import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.ui.panel.RightPanel

object ActionFlowController {

    private var unitSystem: UnitSystem? = null
    private var vw: Int = 0
    private var vh: Int = 0
    private var dockTarget: PlacedObject? = null
    private var dockSlotIndex: Int = -1
    private var isActionMode: Boolean = false
    private var currentUnit: PlacedObject? = null

    fun init(us: UnitSystem) { unitSystem = us }
    fun updateScreenSize(w: Int, h: Int) { vw = w; vh = h }

    data class DockHitResult(
        val target: PlacedObject,
        val slotIndex: Int,
        val sectorX: Float,
        val sectorY: Float,
        val isOccupied: Boolean
    )

    fun findDockTarget(worldX: Float, worldY: Float, mover: PlacedObject, allUnits: List<PlacedObject>): DockHitResult? {
        val us = unitSystem ?: return null
        for (obj in allUnits) {
            if (obj === mover || obj.isDocked) continue
            val hp = us.healthMap[obj] ?: continue
            if (hp <= 0) continue
            val slotCount = DockingSystem.getSlotCount(obj)
            if (slotCount <= 0) continue
            val radius = DockingSystem.getDockRadius(obj)
            val dx = worldX - obj.worldX
            val dy = worldY - obj.worldY
            if (dx * dx + dy * dy > radius * radius) continue
            val clickAngle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
            var angle = clickAngle - obj.heading
            while (angle < 0) angle += 360
            angle %= 360
            val anglePerSlot = 360f / slotCount
            val slotIndex = (angle / anglePerSlot).toInt().coerceIn(0, slotCount - 1)
            val sectors = DockingSystem.getDockingSectors(obj)
            val sector = sectors.firstOrNull { it.index == slotIndex } ?: continue
            val occupied = obj.dockedList.any { it.slotIndex == slotIndex } ||
                           obj.dockingList.any { it.slotIndex == slotIndex }
            return DockHitResult(obj, slotIndex, sector.centerX, sector.centerY, occupied)
        }
        return null
    }

    fun handleMapTap(worldX: Float, worldY: Float): Boolean {
        if (!isActionMode) return false
        if (isUnitMoving()) {
            WarningMessage.show("正在行进中")
            return true
        }
        val us = unitSystem ?: return false
        val unit = us.selectedUnit ?: return false
        val hit = findDockTarget(worldX, worldY, unit, us.placedObjects)
        if (hit != null) {
            if (hit.isOccupied) WarningMessage.show("被占用")
            else onDockTargetSelected(hit.target, hit.slotIndex)
        } else {
            WarningMessage.show("无效目标")
        }
        return true
    }

    fun startAction() {
        val us = unitSystem ?: return
        val unit = us.selectedUnit ?: return
        if (isUnitMoving()) {
            WarningMessage.show("正在行进中")
            return
        }
        // ★ 已停泊的单位不出港，保持现状，确认新目标后再出港
        currentUnit = unit
        isActionMode = true
        ActionPlatform.hide()
        ToastTool.hideGlobal()
        dockTarget = null
        dockSlotIndex = -1
        RenderControl.showUnitInfoPanel = false
        RenderControl.touchDisabledUnitInfoPanel = true
        ToastTool.showGlobal("请选择目标") { cancelAction() }
        ToastTool.globalInstance?.setOffset(0f, -300f)
    }

    private fun isUnitMoving(): Boolean {
        val us = unitSystem ?: return false
        val unit = us.selectedUnit ?: return false
        return unit.isMoving || unit.isStopping || unit.isUndocking
    }

    private fun onDockTargetSelected(target: PlacedObject, slotIndex: Int) {
        val us = unitSystem ?: return
        val unit = us.selectedUnit ?: return
        val targetHp = us.healthMap[target] ?: return
        if (targetHp <= 0 || !us.placedObjects.contains(target)) return

        dockTarget = target
        dockSlotIndex = slotIndex
        val sectors = DockingSystem.getDockingSectors(target)
        val sector = sectors.firstOrNull { it.index == slotIndex } ?: return
        unit.previewTargetX = sector.centerX
        unit.previewTargetY = sector.centerY
        unit.hasPreviewTarget = true
        unit.targetX = sector.centerX
        unit.targetY = sector.centerY
        unit.actionDock = true
        unit.isRotating = true
        unit.isMoving = false
        ActionPlatform.showConfirmCancel(vw, vh, us.context!!) { buttonId ->
            when (buttonId) {
                "confirm" -> confirmAction()
                "cancel" -> cancelDockTarget()
            }
        }
    }

    private fun confirmAction() {
        val us = unitSystem ?: return
        val unit = us.selectedUnit ?: return
        val target = dockTarget ?: return

        val targetHp = us.healthMap[target] ?: return
        if (targetHp <= 0 || !us.placedObjects.contains(target)) {
            cancelDockTarget()
            WarningMessage.show("目标已消失")
            return
        }

        // ★ 如果还停泊着，先出港
        if (unit.isDocked) {
            us.undockShip(unit)
        }

        // ★ 先占用停泊槽位，再设置移动状态
        val dockSuccess = DockingSystem.requestDock(unit, target, dockSlotIndex)
        if (!dockSuccess) {
            WarningMessage.show("停泊点被占用")
            cancelDockTarget()
            return
        }

        // ★ 占用成功后设置移动状态
        unit.isMoving = true
        unit.isStopping = false
        unit.currentSpeed = 0f
        unit.canMove = true
        unit.isRotating = false

        isActionMode = false
        ActionPlatform.hide()
        ToastTool.hideGlobal()
        unit.hasPreviewTarget = false
        unit.actionDock = true
        RenderControl.showUnitInfoPanel = true
        RenderControl.touchDisabledUnitInfoPanel = false
        RightPanel.lockUnit(unit)
        ActionPlatform.showStop(vw, vh, us.context!!) { buttonId ->
            if (buttonId == "stop") stopAction()
        }
    }

    private fun cancelDockTarget() {
        val us = unitSystem ?: return
        val unit = us.selectedUnit ?: return
        unit.hasPreviewTarget = false
        unit.previewTargetX = 0f
        unit.previewTargetY = 0f
        unit.actionDock = false
        unit.isRotating = false
        dockTarget = null
        dockSlotIndex = -1
        ActionPlatform.hide()
    }

    private fun stopAction() {
        val us = unitSystem ?: return
        val unit = us.selectedUnit ?: return
        unit.isMoving = false
        unit.isStopping = true
        unit.targetX = unit.worldX
        unit.targetY = unit.worldY
        unit.dockTarget = null
        unit.actionDock = false
        unit.dockSlotIndex = -1
        unit.hasPreviewTarget = false
        ActionPlatform.hide()
        ToastTool.hideGlobal()
        dockTarget = null
        currentUnit = null
        RenderControl.showUnitInfoPanel = true
        RenderControl.touchDisabledUnitInfoPanel = false
        RightPanel.lockUnit(unit)
    }

    fun forceCleanup() {
        ActionPlatform.hide()
        ToastTool.hideGlobal()
        RenderControl.showUnitInfoPanel = true
        RenderControl.touchDisabledUnitInfoPanel = false
        RenderControl.touchDisabledBottomBar = false
        RenderControl.touchDisabledMultiSelectButton = false
        isActionMode = false
        dockTarget = null
        dockSlotIndex = -1
        currentUnit?.let {
            it.hasPreviewTarget = false
            it.previewTargetX = 0f
            it.previewTargetY = 0f
            it.actionDock = false
            it.isRotating = false
        }
        currentUnit = null
    }

    fun cancelAction() {
        val unit = currentUnit
        forceCleanup()
        unit?.let { RightPanel.lockUnit(it) }
    }

    fun update() {
        val us = unitSystem ?: return
        val unit = us.selectedUnit

        if (unit == null) {
            currentUnit?.let {
                it.hasPreviewTarget = false
                it.previewTargetX = 0f
                it.previewTargetY = 0f
                it.isRotating = false
            }
            if (isActionMode || ActionPlatform.getMode() != ActionPlatform.Mode.NONE) {
                ActionPlatform.hide()
                ToastTool.hideGlobal()
                RenderControl.showUnitInfoPanel = true
                RenderControl.touchDisabledUnitInfoPanel = false
                isActionMode = false
                dockTarget = null
                currentUnit = null
            }
            return
        }

        if (dockTarget != null) {
            val targetHp = us.healthMap[dockTarget] ?: -1f
            if (targetHp <= 0 || !us.placedObjects.contains(dockTarget)) {
                unit.hasPreviewTarget = false
                unit.previewTargetX = 0f
                unit.previewTargetY = 0f
                unit.actionDock = false
                unit.isRotating = false
                unit.isMoving = false
                unit.targetX = unit.worldX
                unit.targetY = unit.worldY
                unit.dockTarget = null
                unit.dockSlotIndex = -1
                dockTarget = null
                dockSlotIndex = -1
                ActionPlatform.hide()
                ToastTool.hideGlobal()
                WarningMessage.show("目标已消失")
            }
        }

        if (unit.isMoving && unit.actionDock && ActionPlatform.getMode() != ActionPlatform.Mode.STOP) {
            ActionPlatform.showStop(vw, vh, us.context!!) { buttonId ->
                if (buttonId == "stop") stopAction()
            }
        }

        if ((unit.isDocked || unit.isStopping) && ActionPlatform.getMode() == ActionPlatform.Mode.STOP) {
            ActionPlatform.hide()
            dockTarget = null
        }
    }
}