package com.rtnp.demo.ai

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import kotlin.math.sqrt
import com.rtnp.demo.logic.MovementSystem

class AiTools(private val unitSystem: UnitSystem) {

    // ==================== 移动指令 ====================

    /** 命令单位移动到指定坐标 */
    fun moveTo(unit: PlacedObject, worldX: Float, worldY: Float): Boolean {
        if (unit.isDocked) {
            unitSystem.undockShip(unit)
        }
        return MovementSystem.requestMove(unit, worldX, worldY)
    }

    /** 命令单位停泊到目标 */
    fun dockTo(unit: PlacedObject, target: PlacedObject): Boolean {
        if (unit.isDocked || unit.autoControl || !unit.canMove) return false
        return com.rtnp.demo.logic.DockingSystem.requestDock(unit, target)
    }

    /** 停止单位移动 */
    fun stopMoving(unit: PlacedObject) {
        unit.isMoving = false
        unit.isStopping = true
        unit.targetX = unit.worldX
        unit.targetY = unit.worldY
        unit.dockTarget = null
        // ★ 不在这里设置 canMove=true，等速度归零时自动恢复
    }

    // ==================== 查询 ====================

    /** 获取所有单位 */
    fun getAllUnits(): List<PlacedObject> = unitSystem.placedObjects.toList()

    /** 根据阵营获取单位 */
    fun getUnitsByFaction(faction: Int): List<PlacedObject> =
        unitSystem.placedObjects.filter { it.faction == faction }

    /** 获取单位血量 */
    fun getHealth(unit: PlacedObject): Float = unitSystem.healthMap[unit] ?: 0f

    /** 计算两点距离 */
    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }

    /** 计算两个单位之间的距离 */
    fun distance(a: PlacedObject, b: PlacedObject): Float {
        return distance(a.worldX, a.worldY, b.worldX, b.worldY)
    }

    /** 寻找最近的敌方单位 */
    fun findNearestEnemy(unit: PlacedObject, range: Float): PlacedObject? {
        var best: PlacedObject? = null
        var bestDist = Float.MAX_VALUE
        for (t in unitSystem.placedObjects) {
            if (t.faction == unit.faction || t.faction == 0) continue
            val hp = unitSystem.healthMap[t] ?: continue
            if (hp <= 0) continue
            val dist = distance(unit, t)
            if (dist <= range && dist < bestDist) {
                bestDist = dist
                best = t
            }
        }
        return best
    }

    /** 寻找最近的友方单位 */
    fun findNearestAlly(unit: PlacedObject, range: Float): PlacedObject? {
        var best: PlacedObject? = null
        var bestDist = Float.MAX_VALUE
        for (t in unitSystem.placedObjects) {
            if (t == unit) continue
            if (t.faction != unit.faction) continue
            val hp = unitSystem.healthMap[t] ?: continue
            if (hp <= 0) continue
            val dist = distance(unit, t)
            if (dist <= range && dist < bestDist) {
                bestDist = dist
                best = t
            }
        }
        return best
    }

    /** 检查单位是否正在移动 */
    fun isMoving(unit: PlacedObject): Boolean = unit.isMoving

    /** 检查单位是否停泊 */
    fun isDocked(unit: PlacedObject): Boolean = unit.isDocked

    /** 获取单位的当前目标 */
    fun getMoveTarget(unit: PlacedObject): Pair<Float, Float> {
        return Pair(unit.targetX, unit.targetY)
    }

    /** 获取单位的锁定攻击目标 */
    fun getAttackTarget(unit: PlacedObject, slotIndex: Int): PlacedObject? {
        if (slotIndex < 0 || slotIndex >= unit.weaponSlots.size) return null
        return unit.weaponSlots[slotIndex].lockedTarget
    }

    // ==================== 战斗指令 ====================

    /** 设置武器槽锁定目标 */
    fun setAttackTarget(unit: PlacedObject, slotIndex: Int, target: PlacedObject?) {
        if (slotIndex < 0 || slotIndex >= unit.weaponSlots.size) return
        unit.weaponSlots[slotIndex].lockedTarget = target
    }

    /** 激活所有武器 */
    fun activateAllWeapons(unit: PlacedObject) {
        for (ws in unit.weaponSlots) {
            ws.active = true
        }
        unit.syncCurrentWeapon()
    }

    // ==================== 世界信息 ====================

    /** 世界宽度 */
    val worldWidth: Float = 3000f

    /** 世界高度 */
    val worldHeight: Float = 5000f

    /** 世界中心 */
    val worldCenterX: Float = 1500f
    val worldCenterY: Float = 2500f
}