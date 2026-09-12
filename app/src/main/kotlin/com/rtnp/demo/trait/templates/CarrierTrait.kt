// app/src/main/kotlin/com/rtnp/demo/trait/templates/CarrierTrait.kt
package com.rtnp.demo.trait.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.trait.TraitTemplate
import com.rtnp.demo.trait.TraitTools
import kotlin.math.sqrt

class CarrierTrait : TraitTemplate() {

    override val id = "carrier"
    override val displayName = "运载"
    override val description = "前往指定坐标后放置单位并自毁"

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: TraitTools) {
        // effectAngleOffset: 0=未开始, 1=已设置目标, 2=已完成
        if (host.effectAngleOffset == 2f) return

        val task = tools.getTaskData(host) ?: return
        val status = task.getString("status") ?: return
        if (status != "flying" && status != "moving") return

        val targetX = task.getFloat("targetX") ?: return
        val targetY = task.getFloat("targetY") ?: return
        val spawnUnitName = task.getString("spawnUnitName") ?: "漂泊信标"

        // 设置移动目标
        if (host.effectAngleOffset != 1f) {
            host.targetX = targetX
            host.targetY = targetY
            host.isMoving = true
            host.isStopping = false
            host.currentSpeed = 0f
            host.canMove = true
            host.isRotating = false
            host.acceleration = 20f
            host.maxSpeed = 160f
            host.turnRate = 180f
            host.effectAngleOffset = 1f
            return
        }

        // 检测到达（距离 < 10 像素）
        val dx = host.worldX - targetX
        val dy = host.worldY - targetY
        if (sqrt(dx * dx + dy * dy) < 10f) {
            host.effectAngleOffset = 2f
            // 生成信标
            tools.spawnUnit(spawnUnitName, host.worldX, host.worldY)
            // 完成任务
            tools.completeTask(task.taskId)
            // 自毁
            tools.removeUnit(host)
        }
    }
}