package com.rtnp.demo.logic

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.movement.ItemManager

/**
 * 采矿系统：管理单位对矿物的采集
 */
object MiningSystem {

    private val timers = mutableMapOf<PlacedObject, Float>()

    fun update(dt: Float, allUnits: MutableList<PlacedObject>) {
        // 清理无效计时器
        val timerIt = timers.iterator()
        while (timerIt.hasNext()) {
            val entry = timerIt.next()
            if (!allUnits.contains(entry.key)) {
                timerIt.remove()
            }
        }

        for (unit in allUnits) {
            val mineral = unit.miningTarget ?: continue

            if (!unit.miningEnabled || unit.miningSpeed <= 0) {
                stopMining(unit)
                continue
            }

            if (!allUnits.contains(mineral) || mineral.storedItemCount <= 0 ||
                mineral.storedItemName.isNullOrEmpty()) {
                stopMining(unit)
                continue
            }

            // 如果单位在移动，或者虽然停泊但停泊目标不是当前采矿目标，则停止采矿
            if (unit.isMoving || (unit.isDocked && unit.dockedAt != unit.miningTarget)) {
                stopMining(unit)
                continue
            }

            val dx = unit.worldX - mineral.worldX
            val dy = unit.worldY - mineral.worldY
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            val effectiveRange = mineral.size / 2f + maxOf(unit.width, unit.height) / 2f + 40f
            if (dist > effectiveRange) {
                stopMining(unit)
                continue
            }

            if (!unit.isMining) {
                unit.isMining = true
                timers[unit] = 0f
            }

            var timer = timers[unit] ?: 0f
            timer += dt

            while (timer >= 1.0f) {
                timer -= 1.0f

                val gatherCount = unit.miningSpeed.toInt()
                val itemName = mineral.storedItemName ?: break
                val available = mineral.storedItemCount
                var taken = minOf(gatherCount, available)
                if (taken <= 0) {
                    stopMining(unit)
                    break
                }

                val def = ItemManager.getInstance().getItemDef(itemName)
                val spacePerItem = def?.space ?: 1
                val spaceNeeded = taken * spacePerItem

                val currentStored = getTotalStored(unit)
                val spaceLeft = unit.storageCapacity - currentStored
                if (spaceLeft <= 0) {
                    stopMining(unit)
                    break
                }
                if (spaceNeeded > spaceLeft) {
                    taken = spaceLeft / spacePerItem
                    if (taken <= 0) {
                        stopMining(unit)
                        break
                    }
                }

                mineral.storedItemCount -= taken
                if (mineral.storedItemCount < 0) mineral.storedItemCount = 0

                if (unit.collectedItems == null) {
                    unit.collectedItems = mutableMapOf()
                }
                val already = unit.collectedItems?.get(itemName) ?: 0
                unit.collectedItems?.put(itemName, already + taken)

                if (mineral.storedItemCount <= 0) {
                    for (u in allUnits) {
                        if (u.isMining && u.miningTarget == mineral) {
                            stopMining(u)
                        }
                    }
                    break
                }
            }
            timers[unit] = timer
        }
    }

    fun forceStartMining(unit: PlacedObject, allUnits: List<PlacedObject>) {
        if (unit == null) return
        val mineral = unit.miningTarget ?: return

        if (!allUnits.contains(mineral) || mineral.storedItemCount <= 0 ||
            mineral.storedItemName.isNullOrEmpty()) {
            unit.miningTarget = null
            return
        }

        val dx = unit.worldX - mineral.worldX
        val dy = unit.worldY - mineral.worldY
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val effectiveRange = mineral.size / 2f + maxOf(unit.width, unit.height) / 2f + 40f
        if (dist > effectiveRange) {
            unit.miningTarget = null
            return
        }

        unit.isMoving = false
        unit.isStopping = false
        unit.currentSpeed = 0f
        unit.isMining = true
        timers[unit] = 0f
    }

    fun stopMining(unit: PlacedObject) {
        unit.isMining = false
        unit.miningTarget = null
        timers.remove(unit)
    }

    private fun getTotalStored(unit: PlacedObject): Int {
        val items = unit.collectedItems ?: return 0
        var total = 0
        for ((name, count) in items) {
            val def = ItemManager.getInstance().getItemDef(name)
            val space = def?.space ?: 1
            total += count * space
        }
        return total
    }
}