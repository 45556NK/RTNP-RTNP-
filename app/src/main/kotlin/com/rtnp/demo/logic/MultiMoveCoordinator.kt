// app/src/main/kotlin/com/rtnp/demo/logic/MultiMoveCoordinator.kt
package com.rtnp.demo.logic

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject

/**
 * 多选移动协调器：支持统一移动和批量停泊
 */
object MultiMoveCoordinator {

    private var active = false
    private var mode = Mode.MOVE  // 当前模式
    private var targetX = 0f
    private var targetY = 0f
    private var dockTarget: PlacedObject? = null  // 批量停泊目标
    private val waitingUnits = mutableListOf<PlacedObject>()
    private val unitTargets = mutableMapOf<Long, Pair<Float, Float>>()  // 单位ID -> 目标坐标

    private enum class Mode { MOVE, DOCK }

    /**
     * 发起多选移动：所有选中单位先出港，等全部准备好后一起出发到同一目标
     */
    fun startMove(host: UnitSystem, worldX: Float, worldY: Float) {
        val selected = host.multiSelectManager?.getSelectedUnits() ?: return
        if (selected.isEmpty()) return

        mode = Mode.MOVE
        targetX = worldX
        targetY = worldY
        dockTarget = null
        unitTargets.clear()
        waitingUnits.clear()
        waitingUnits.addAll(selected)
        active = true

        for (unit in waitingUnits) {
            if (unit.type == "base") continue
            if (unit.isDocked) {
                unit.isUndocking = true
                unit.undockProgress = 0f
            }
            unit.dockTarget = null
            unit.miningTarget = null
        }
    }

    /**
     * ★ 批量停泊：为每个选中单位分配停泊槽位
     * @return 成功分配的数量，-1 表示目标无可用槽位
     */
    fun startBatchDock(host: UnitSystem, target: PlacedObject): Int {
        val selected = host.multiSelectManager?.getSelectedUnits() ?: return 0
        if (selected.isEmpty()) return 0

        val totalSlots = DockingSystem.getSlotCount(target)
        if (totalSlots <= 0) return -1

        // 收集已占用槽位
        val occupiedSlots = mutableSetOf<Int>()
        for (entry in target.dockedList) { occupiedSlots.add(entry.slotIndex) }
        for (entry in target.dockingList) { occupiedSlots.add(entry.slotIndex) }

        val sectors = DockingSystem.getDockingSectors(target)
        mode = Mode.DOCK
        dockTarget = target
        unitTargets.clear()
        waitingUnits.clear()
        active = true

        var assigned = 0
        for (unit in selected) {
            if (unit.type == "base") continue
            if (unit.isDocked && unit.dockedAt == target) continue // 已经停泊在此

            // 找一个空闲槽位
            val freeSlot = (0 until totalSlots).firstOrNull { it !in occupiedSlots } ?: break
            occupiedSlots.add(freeSlot)

            // 占用槽位
            target.dockingList.add(PlacedObject.DockEntry(unit.uniqueId, freeSlot))

            // 找到槽位坐标
            val sector = sectors.firstOrNull { it.index == freeSlot } ?: continue
            unitTargets[unit.uniqueId] = Pair(sector.centerX, sector.centerY)

            // 启动出港
            if (unit.isDocked) {
                unit.isUndocking = true
                unit.undockProgress = 0f
            }

            unit.dockTarget = target
            unit.dockSlotIndex = freeSlot
            unit.actionDock = true
            unit.dockTarget = target
            unit.miningTarget = null

            waitingUnits.add(unit)
            assigned++
        }

        return assigned
    }

    /**
     * 每帧检查是否全部就绪
     */
    fun update() {
        if (!active) return

        var allReady = true
        for (unit in waitingUnits) {
            if (unit.type == "base") continue
            if (unit.isUndocking) {
                allReady = false
                break
            }
        }

        if (allReady) {
            for (unit in waitingUnits) {
                if (unit.type == "base") continue
                unit.isMoving = false
                unit.isStopping = false
                unit.currentSpeed = 0f

                if (mode == Mode.DOCK) {
                    // 批量停泊：移动到分配的槽位
                    val pos = unitTargets[unit.uniqueId]
                    if (pos != null) {
                        unit.targetX = pos.first
                        unit.targetY = pos.second
                        unit.actionDock = true
                    }
                } else {
                    // 统一移动
                    unit.targetX = targetX
                    unit.targetY = targetY
                    unit.actionDock = false
                }

                unit.canMove = true
                unit.isMoving = true
            }
            waitingUnits.clear()
            unitTargets.clear()
            dockTarget = null
            active = false
        }
    }

    fun isActive(): Boolean = active

    fun cancel() {
        // 释放已占用的槽位
        val target = dockTarget
        if (target != null && mode == Mode.DOCK) {
            for (unit in waitingUnits) {
                target.dockingList.removeAll { it.unitId == unit.uniqueId }
            }
        }
        active = false
        waitingUnits.clear()
        unitTargets.clear()
        dockTarget = null
    }
}