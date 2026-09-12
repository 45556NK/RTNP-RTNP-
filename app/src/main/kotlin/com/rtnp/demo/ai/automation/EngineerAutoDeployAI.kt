// app/src/main/kotlin/com/rtnp/demo/ai/automation/EngineerAutoDeployAI.kt
package com.rtnp.demo.ai.automation

import com.rtnp.demo.ai.AiTemplate
import com.rtnp.demo.ai.AiTools
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.BuildManager
import com.rtnp.demo.logic.DockingSystem
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.logger.Logger

object EngineerAutoDeployAI : AiTemplate {

    override val id = "engineer_auto_deploy"
    override val displayName = "工程自动化部署"
    override val description = "自动为当前区块的建造中建筑提供材料并停泊协助建造"
    override val enabledByDefault = true

    private data class State(
        var targetBuild: PlacedObject? = null,
        var targetMineral: PlacedObject? = null,
        var phase: Phase = Phase.IDLE
    )

    private enum class Phase {
        IDLE,
        GOING_TO_BUILD,
        GOING_TO_MINE,
        MINING,
        BUILDING
    }

    private val states = mutableMapOf<Long, State>()

    fun clearState(unit: PlacedObject) {
        states.remove(unit.uniqueId)
    }

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: AiTools) {
        val state = states.getOrPut(host.uniqueId) { State() }

        if (host.isMoving) return

        if (host.isDocked) {
            when (state.phase) {
                Phase.GOING_TO_BUILD, Phase.BUILDING -> {
                    if (host.dockedAt == state.targetBuild) {
                        state.phase = Phase.BUILDING
                        val process = BuildManager.getBuildProcesses().firstOrNull { it.invisibleUnit == state.targetBuild }
                        if (process == null || process.progress >= process.item.buildAmount) {
                            state.targetBuild = null
                            state.phase = Phase.IDLE
                        }
                    } else {
                        releaseAndIdle(host)
                    }
                }
                Phase.GOING_TO_MINE, Phase.MINING -> {
                    if (host.dockedAt == state.targetMineral) {
                        state.phase = Phase.MINING
                        val needed = getMissingResources(host, state.targetBuild)
                        if (needed <= 0) {
                            state.phase = Phase.GOING_TO_BUILD
                            state.targetMineral = null
                            releaseAndGoToBuild(host, state.targetBuild)
                        } else {
                            val mineral = state.targetMineral
                            if (mineral == null || mineral.storedItemCount <= 0) {
                                state.targetMineral = null
                                state.phase = Phase.IDLE
                            }
                        }
                    } else {
                        releaseAndIdle(host)
                    }
                }
                else -> {
                    releaseAndIdle(host)
                }
            }
            return
        }

        when (state.phase) {
            Phase.IDLE -> {
                val build = findBuildTarget(host, tools)
                if (build != null) {
                    state.targetBuild = build
                    // 检查是否有足够资源
                    if (getMissingResources(host, build) <= 0) {
                        state.phase = Phase.GOING_TO_BUILD
                        DockingSystem.requestDock(host, build)
                    } else {
                        val mineral = findMineral(host, build, tools)
                        if (mineral != null) {
                            state.targetMineral = mineral
                            state.phase = Phase.GOING_TO_MINE
                            DockingSystem.requestDock(host, mineral)
                        } else {
                            state.targetBuild = null
                        }
                    }
                } else {
                    state.targetBuild = null
                }
            }
            Phase.BUILDING -> {
                if (state.targetBuild != null) {
                    state.phase = Phase.GOING_TO_BUILD
                    DockingSystem.requestDock(host, state.targetBuild!!)
                } else {
                    state.phase = Phase.IDLE
                }
            }
            Phase.GOING_TO_BUILD, Phase.GOING_TO_MINE, Phase.MINING -> {
                state.phase = Phase.IDLE
            }
        }
    }

    private fun releaseAndIdle(host: PlacedObject) {
        DockingSystem.releaseSlot(host)
        host.isDocked = false
        host.dockedAt = null
        states[host.uniqueId]?.phase = Phase.IDLE
    }

    private fun releaseAndGoToBuild(host: PlacedObject, build: PlacedObject?) {
        DockingSystem.releaseSlot(host)
        host.isDocked = false
        host.dockedAt = null
        if (build != null) {
            DockingSystem.requestDock(host, build)
        }
    }

    private fun getMissingResources(host: PlacedObject, build: PlacedObject?): Int {
        if (build == null) return 0
        val process = BuildManager.getBuildProcesses().firstOrNull { it.invisibleUnit == build } ?: return 0
        val costItem = process.item.costItem ?: return 0
        val costAmount = process.item.costAmount
        val submitted = process.submittedMaterials
        val needed = costAmount - submitted
        if (needed <= 0) return 0
        val have = host.collectedItems?.get(costItem) ?: 0
        return (needed - have).coerceAtLeast(0)
    }

    private fun findBuildTarget(host: PlacedObject, tools: AiTools): PlacedObject? {
        val tileId = HexGridManager.getTileIdForUnit(host) ?: return null
        val units = HexGridManager.getUnitsInTile(tileId)
        for (unit in units) {
            if (unit.category == "建造中单位" && unit.faction == host.faction) {
                val process = BuildManager.getBuildProcesses().firstOrNull { it.invisibleUnit == unit }
                if (process != null && process.progress < process.item.buildAmount) {
                    val slots = DockingSystem.getSlotCount(unit)
                    val occupied = unit.dockedList.size + unit.dockingList.size
                    if (occupied < slots) return unit
                }
            }
        }
        return null
    }

    private fun findMineral(host: PlacedObject, build: PlacedObject, tools: AiTools): PlacedObject? {
        val process = BuildManager.getBuildProcesses().firstOrNull { it.invisibleUnit == build } ?: return null
        val costItem = process.item.costItem ?: return null
        val tileId = HexGridManager.getTileIdForUnit(host) ?: return null
        val units = HexGridManager.getUnitsInTile(tileId)
        var best: PlacedObject? = null
        var bestDist = Float.MAX_VALUE
        for (unit in units) {
            if (unit.type != "environment") continue
            if (unit.storedItemName != costItem) continue
            if (unit.storedItemCount <= 0) continue
            val dist = tools.distance(host, unit)
            if (dist < bestDist) {
                bestDist = dist
                best = unit
            }
        }
        return best
    }
}