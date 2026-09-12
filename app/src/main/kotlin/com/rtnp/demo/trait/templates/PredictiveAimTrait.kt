package com.rtnp.demo.trait.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.trait.TraitTemplate
import com.rtnp.demo.trait.TraitTools
import kotlin.math.*

class PredictiveAimTrait : TraitTemplate() {

    override val id = "predictive_aim"
    override val displayName = "预瞄火控"
    override val description = "轨道炮自动计算提前量，提高对移动目标的命中率"

    companion object {
        @JvmField var debugLog: String = ""

        private fun normalizeAngle(a: Float): Float {
            var angle = a
            while (angle > PI) angle -= 2f * PI.toFloat()
            while (angle < -PI) angle += 2f * PI.toFloat()
            return angle
        }
    }

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: TraitTools) {
        val sb = StringBuilder()
        sb.append("预瞄: ")

        if (host.weaponSlots.isEmpty()) {
            sb.append("无武器槽")
            debugLog = sb.toString()
            return
        }

        for (ws in host.weaponSlots) {
            if (ws.type != "railgun" || !ws.active) {
                ws.aimAngleOffset = 0f
                continue
            }

            val target = ws.lockedTarget
            if (target == null || !tools.isValidTarget(host, target)) {
                ws.aimAngleOffset = 0f
                sb.append("无目标 ")
                continue
            }

            if (target.currentSpeed <= 0f) {
                ws.aimAngleOffset = 0f
                sb.append("静止 ")
                continue
            }

            val bulletSpeed = if (ws.bulletSpeed > 0) ws.bulletSpeed else 600f
            val dx = target.worldX - host.worldX
            val dy = target.worldY - host.worldY
            val dist = sqrt(dx * dx + dy * dy)
            if (dist <= 0f) continue

            val bulletTravelTime = dist / bulletSpeed
            val targetHeadingRad = Math.toRadians(target.heading.toDouble()).toFloat()
            val predictedX = target.worldX + cos(targetHeadingRad) * target.currentSpeed * bulletTravelTime
            val predictedY = target.worldY + sin(targetHeadingRad) * target.currentSpeed * bulletTravelTime

            val directAngle = atan2(dy, dx)
            val aimAngle = atan2(predictedY - host.worldY, predictedX - host.worldX)
            ws.aimAngleOffset = normalizeAngle(aimAngle - directAngle)

            sb.append("偏移=${String.format("%.1f", Math.toDegrees(ws.aimAngleOffset.toDouble()))}° ")
        }

        debugLog = sb.toString()
    }
}