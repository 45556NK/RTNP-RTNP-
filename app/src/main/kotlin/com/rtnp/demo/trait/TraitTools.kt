// app/src/main/kotlin/com/rtnp/demo/trait/TraitTools.kt
package com.rtnp.demo.trait

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.strategy.StrategyDataBus
import kotlin.math.sqrt

class TraitTools {

    val worldWidth: Float get() = 3000f
    val worldHeight: Float get() = 5000f

    var healthMap: Map<PlacedObject, Float> = emptyMap()
    var removeUnitCallback: ((PlacedObject) -> Unit)? = null
    var spawnUnitCallback: ((String, Float, Float) -> PlacedObject?)? = null

    // ★ 死亡回调注册表（模块化清理）
    private val deathCallbacks = mutableMapOf<Long, MutableList<(PlacedObject) -> Unit>>()

    fun registerDeathCallback(unit: PlacedObject, callback: (PlacedObject) -> Unit) {
        deathCallbacks.getOrPut(unit.uniqueId) { mutableListOf() }.add(callback)
    }

    fun executeDeathCallbacks(unit: PlacedObject) {
        deathCallbacks[unit.uniqueId]?.forEach { it(unit) }
        deathCallbacks.remove(unit.uniqueId)
    }

    fun spawnUnit(name: String, worldX: Float, worldY: Float): PlacedObject? {
        return spawnUnitCallback?.invoke(name, worldX, worldY)
    }

    fun removeUnit(unit: PlacedObject) {
        removeUnitCallback?.invoke(unit)
    }

    fun distance(a: PlacedObject, b: PlacedObject): Float {
        val dx = a.worldX - b.worldX
        val dy = a.worldY - b.worldY
        return sqrt(dx * dx + dy * dy)
    }

    fun distanceSq(a: PlacedObject, b: PlacedObject): Float {
        val dx = a.worldX - b.worldX
        val dy = a.worldY - b.worldY
        return dx * dx + dy * dy
    }

    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }

    fun healthRatio(unit: PlacedObject): Float {
        val hp = healthMap[unit] ?: return 0f
        return (hp / unit.originalHealth).coerceIn(0f, 1f)
    }

    fun isValidTarget(attacker: PlacedObject, target: PlacedObject): Boolean {
        if (target.faction == attacker.faction || target.faction == 0) return false
        val hp = healthMap[target] ?: return false
        return hp > 0
    }

    fun getTaskData(unit: PlacedObject): StrategyDataBus.TaskData? {
        return StrategyDataBus.getTaskData(unit)
    }

    fun bindToTask(unit: PlacedObject, taskId: Long) {
        StrategyDataBus.bindUnitToTask(unit, taskId)
    }

    fun createTask(data: MutableMap<String, Any?>): Long {
        return StrategyDataBus.createTask(data)
    }

    fun updateTaskData(taskId: Long, key: String, value: Any?) {
        StrategyDataBus.updateTaskData(taskId, key, value)
    }

    fun completeTask(taskId: Long) {
        StrategyDataBus.completeTask(taskId)
    }
}