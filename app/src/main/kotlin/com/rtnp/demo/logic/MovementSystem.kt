package com.rtnp.demo.logic

import com.rtnp.demo.combat.WeaponConstants
import com.rtnp.demo.core.PlacedObject
import kotlin.math.abs

object MovementSystem {

    private const val ANGLE_TOLERANCE = 1f
    private const val MIN_DECELERATION = 10f

    fun update(deltaTime: Float, units: MutableList<PlacedObject>) {
        val speedMult = WeaponConstants.cooldownMultiplier
        for (unit in units) {
            if ("base" == unit.type) continue

            // ★ 停泊单位只允许原地转向，不允许移动
            if (unit.isDocked) {
                if (unit.isRotating) {
                    rotateToward(unit, unit.targetX, unit.targetY, deltaTime, speedMult)
                }
                continue
            }

            if (!unit.canMove) continue

            // ========== 转向中（不移动，只旋转） ==========
            if (unit.isRotating) {
                rotateToward(unit, unit.targetX, unit.targetY, deltaTime, speedMult)
                continue
            }

            // ========== 减速停止 ==========
            if (unit.isStopping) {
                val effectiveDecel = if (unit.deceleration > 0) unit.deceleration else MIN_DECELERATION
                unit.currentSpeed -= effectiveDecel * deltaTime * speedMult
                if (unit.currentSpeed <= 0) {
                    unit.currentSpeed = 0f
                    unit.isStopping = false
                    unit.isMoving = false
                    unit.canMove = true
                    continue
                }
                val rad = Math.toRadians(unit.heading.toDouble())
                val step = unit.currentSpeed * deltaTime * speedMult
                unit.worldX += (Math.cos(rad) * step).toFloat()
                unit.worldY += (Math.sin(rad) * step).toFloat()
                continue
            }

            // ========== 普通移动 ==========
            if (!unit.isMoving) continue

            // 先转向
            val dx = unit.targetX - unit.worldX
            val dy = unit.targetY - unit.worldY
            val targetAngle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()

            if (!rotateToward(unit, unit.targetX, unit.targetY, deltaTime, speedMult)) {
                // 还没对准，不移动
                continue
            }

            // 对准了，加速移动
            if (unit.currentSpeed < unit.maxSpeed) {
                unit.currentSpeed += unit.acceleration * deltaTime * speedMult
                if (unit.currentSpeed > unit.maxSpeed) unit.currentSpeed = unit.maxSpeed
            }

            val rad = Math.toRadians(unit.heading.toDouble())
            val step = unit.currentSpeed * deltaTime * speedMult
            unit.worldX += (Math.cos(rad) * step).toFloat()
            unit.worldY += (Math.sin(rad) * step).toFloat()

            val toX = unit.targetX - unit.worldX
            val toY = unit.targetY - unit.worldY
            val dist = Math.sqrt((toX * toX + toY * toY).toDouble()).toFloat()
            if (dist <= step) {
                unit.worldX = unit.targetX
                unit.worldY = unit.targetY
                unit.isMoving = false
                unit.currentSpeed = 0f
                unit.canMove = true
            }
        }
    }

    /**
     * 让单位向目标旋转
     * @return true 表示已对准目标
     */
    private fun rotateToward(unit: PlacedObject, targetX: Float, targetY: Float, deltaTime: Float, speedMult: Float): Boolean {
        val dx = targetX - unit.worldX
        val dy = targetY - unit.worldY
        val targetAngle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()

        var diff = targetAngle - unit.heading
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360

        if (abs(diff) <= ANGLE_TOLERANCE) {
            unit.heading = targetAngle
            return true
        }

        val maxTurn = unit.turnRate * deltaTime * speedMult
        if (abs(diff) <= maxTurn) {
            unit.heading = targetAngle
            return true
        } else {
            unit.heading += Math.signum(diff) * maxTurn
        }
        if (unit.heading < 0) unit.heading += 360
        if (unit.heading >= 360) unit.heading -= 360
        return false
    }

    fun requestMove(unit: PlacedObject, targetX: Float, targetY: Float): Boolean {
        if (unit.isMoving || unit.isStopping) return false
        if (unit.isDocked) return false

        unit.targetX = targetX
        unit.targetY = targetY
        unit.isMoving = true
        unit.isStopping = false
        unit.currentSpeed = 0f
        unit.dockTarget = null
        unit.miningTarget = null
        unit.actionDock = false
        return true
    }
}