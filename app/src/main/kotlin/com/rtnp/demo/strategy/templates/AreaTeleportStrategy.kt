package com.rtnp.demo.strategy.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.DockingSystem
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.strategy.StrategyTemplate
import com.rtnp.demo.strategy.StrategyTools
import com.rtnp.demo.logger.Logger

import kotlin.math.sqrt

class AreaTeleportStrategy : StrategyTemplate() {
    override val id = "area_teleport"
    override val displayName = "区域传送"
    override val description = "创建5秒后将半径100以内与宿主\n同阵营的普通单位传送到附近7区块内同阵营建筑\n只生效一次"
    override val cooldown = 0f
    override val hiddenFromPlayer = true
    override val blacklistTypes: List<String> = listOf("environment", "missile")

    private val chargeDuration = 5f
    private val effectRadius = 100f
    private val ringThickness = 6f

    private val progressCircleIds = mutableMapOf<Long, Long>()
    private val teleportPathIds = mutableMapOf<Long, Long>()

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: StrategyTools) {
        val hostId = host.uniqueId
        if (host.effectOffsetX == 1f) return

        if (!progressCircleIds.containsKey(hostId)) {
            val lockedTarget = findTeleportTarget(host, tools)
            if (lockedTarget == null) return

            host.effectOffsetY = lockedTarget.uniqueId.toFloat()

            val startMarkerRadius = host.referenceVolume?.radius ?: 20f

            val pathId = com.rtnp.demo.gpu.render.tools.TeleportPathSystem.addTeleportPath(
                startX = host.worldX, startY = host.worldY,
                endX = lockedTarget.worldX, endY = lockedTarget.worldY,
                color = floatArrayOf(0.6f, 0.8f, 1f),
                pulseDuration = 0.062f,
                startMarkerRadius = startMarkerRadius
            )
            Logger.d("AreaTeleport", "refRadius=${host.referenceVolume?.radius}, startMarkerRadius=$startMarkerRadius")
            teleportPathIds[hostId] = pathId

            val id = tools.getProgressCircleSystem().addProgressCircle(
                host.worldX, host.worldY, effectRadius,
                floatArrayOf(200f / 255f, 150f / 255f, 1f, 60f / 255f),
                floatArrayOf(128f / 255f, 0f, 1f, 1f),
                chargeDuration,
                ringThickness
            ) {
                host.effectOffsetX = 1f
                val pid = progressCircleIds.remove(hostId) ?: -1L
                tools.getProgressCircleSystem().removeProgressCircle(pid)
                executeTeleport(host, tools)
            }
            progressCircleIds[hostId] = id
        }

        val id = progressCircleIds[hostId] ?: return
        tools.getProgressCircleSystem().updateCirclePosition(id, host.worldX, host.worldY)
    }

    private fun findTeleportTarget(host: PlacedObject, tools: StrategyTools): PlacedObject? {
        val hostTileId = HexGridManager.getTileIdForUnit(host) ?: return null
        val allNearbyUnits = HexGridManager.getUnitsInTileAndNeighbors(hostTileId)
        var best: PlacedObject? = null
        var bestDist = Float.MAX_VALUE
        for (unit in allNearbyUnits) {
            if (unit.type != "base") continue
            if (unit.category == "临时建筑" || unit.category == "建造中单位") continue
            if (unit.faction != host.faction) continue
            if (unit.health <= 0) continue
            if (DockingSystem.getSlotCount(unit) <= 0) continue
            val dx = unit.worldX - host.worldX
            val dy = unit.worldY - host.worldY
            val dist = dx * dx + dy * dy
            if (dist < bestDist) {
                bestDist = dist
                best = unit
            }
        }
        return best
    }

    private fun executeTeleport(host: PlacedObject, tools: StrategyTools) {
        val hostId = host.uniqueId
        teleportPathIds.remove(hostId)?.let {
            com.rtnp.demo.gpu.render.tools.TeleportPathSystem.removeTeleportPath(it)
        }

        val targetId = host.effectOffsetY.toLong()
        val target = tools.getAllUnits().firstOrNull { it.uniqueId == targetId } ?: return

        val allyUnits = mutableListOf<PlacedObject>()
        for (unit in tools.getAllUnits()) {
            if (unit == host) continue
            if (unit.faction != host.faction) continue
            if (unit.health <= 0) continue
            val dx = host.worldX - unit.worldX
            val dy = host.worldY - unit.worldY
            if (sqrt(dx * dx + dy * dy) <= effectRadius) {
                allyUnits.add(unit)
            }
        }
        if (allyUnits.isEmpty()) return

        val normalUnits = allyUnits.filter { unit ->
            unit.type != "base" && unit.type != "environment" &&
            unit.category != "临时建筑" && unit.category != "建造中单位" &&
            unit.category != "missile"
        }
        if (normalUnits.isEmpty()) return

        for (unit in normalUnits) {
            teleportUnit(unit, listOf(target))
        }
    }

    private fun teleportUnit(unit: PlacedObject, bases: List<PlacedObject>): Boolean {
        if (unit.isDocked) {
            DockingSystem.releaseSlot(unit)
            unit.dockedAt = null
            unit.isDocked = false
        }
        unit.isMoving = false
        unit.isStopping = false
        unit.currentSpeed = 0f

        val sortedBases = bases.sortedBy { base ->
            val dx = unit.worldX - base.worldX
            val dy = unit.worldY - base.worldY
            dx * dx + dy * dy
        }
        for (base in sortedBases) {
            val success = DockingSystem.requestDock(unit, base)
            if (!success) continue
            val sectors = DockingSystem.getDockingSectors(base)
            val sector = sectors.firstOrNull { it.index == unit.dockSlotIndex }
            if (sector != null) {
                unit.worldX = sector.centerX
                unit.worldY = sector.centerY
                unit.targetX = sector.centerX
                unit.targetY = sector.centerY
            } else {
                unit.worldX = base.worldX
                unit.worldY = base.worldY
                unit.targetX = base.worldX
                unit.targetY = base.worldY
            }
            unit.isDocked = true
            unit.dockedAt = base
            unit.dockTarget = null
            unit.actionDock = false
            base.dockingList.removeAll { it.unitId == unit.uniqueId }
            base.dockedList.add(PlacedObject.DockEntry(unit.uniqueId, unit.dockSlotIndex))
            return true
        }
        return false
    }

    override fun onClicked(host: PlacedObject, tools: StrategyTools, slotIndex: Int) {}
}