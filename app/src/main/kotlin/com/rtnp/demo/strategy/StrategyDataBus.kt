// app/src/main/kotlin/com/rtnp/demo/strategy/StrategyDataBus.kt
package com.rtnp.demo.strategy

import com.rtnp.demo.core.PlacedObject

/**
 * 策略间数据总线：通过任务ID让不同策略进行通讯
 * 每个任务有唯一ID，数据隔离，多任务可同时存在
 */
object StrategyDataBus {

    /** 任务ID -> 任务数据 */
    private val tasks = mutableMapOf<Long, TaskData>()

    /** 单位ID -> 任务ID（一个单位只能关联一个任务） */
    private val unitToTask = mutableMapOf<Long, Long>()

    /**
     * 创建新任务
     * @return 任务ID
     */
    fun createTask(data: MutableMap<String, Any?>): Long {
        val taskId = System.nanoTime() + (Math.random() * 1000).toLong()
        tasks[taskId] = TaskData(taskId, data)
        return taskId
    }

    /**
     * 将单位绑定到任务
     */
    fun bindUnitToTask(unit: PlacedObject, taskId: Long) {
        unitToTask[unit.uniqueId] = taskId
    }

    /**
     * 获取单位关联的任务数据
     */
    fun getTaskData(unit: PlacedObject): TaskData? {
        val taskId = unitToTask[unit.uniqueId] ?: return null
        return tasks[taskId]
    }

    /**
     * 获取任务数据
     */
    fun getTaskData(taskId: Long): TaskData? = tasks[taskId]

    /**
     * 更新任务数据
     */
    fun updateTaskData(taskId: Long, key: String, value: Any?) {
        tasks[taskId]?.data?.put(key, value)
    }

    /**
     * 完成任务，清理数据
     */
    fun completeTask(taskId: Long) {
        tasks.remove(taskId)
        // 清理绑定
        unitToTask.entries.removeAll { it.value == taskId }
    }

    /**
     * 解绑单位
     */
    fun unbindUnit(unit: PlacedObject) {
        unitToTask.remove(unit.uniqueId)
    }

    /**
     * 任务数据容器
     */
    data class TaskData(
        val taskId: Long,
        val data: MutableMap<String, Any?>
    ) {
        fun getString(key: String): String? = data[key] as? String
        fun getInt(key: String): Int? = data[key] as? Int
        fun getLong(key: String): Long? = data[key] as? Long
        fun getFloat(key: String): Float? = data[key] as? Float
    }
}