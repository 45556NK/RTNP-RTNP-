package com.rtnp.demo.gpu.effect.templates

import com.rtnp.demo.gpu.effect.EffectLayer
import com.rtnp.demo.gpu.effect.GpuEffectInstance
import com.rtnp.demo.gpu.effect.GpuEffectTemplate
import kotlin.math.cos
import kotlin.math.sin

class TailFlameEffect : GpuEffectTemplate {
    override val id = "tail_flame_unit"
    override val shaderToolId = "tail_flame"
    override val defaultDuration = 999f
    override val layer = EffectLayer.UNDER_UNITS
    override val useWorldSpace = true
    override val followHost = true
    override val followUnit = true   // 让尾焰跟随单位渲染

    override fun isAlive(instance: GpuEffectInstance): Boolean {
        val host = instance.hostUnit ?: return false
        return host.currentSpeed > 0f && host.health > 0 && !host.isStopping
    }

    override fun getWorldBounds(instance: GpuEffectInstance): FloatArray? {
        val host = instance.hostUnit ?: return null
        val size = maxOf(host.width.toFloat(), host.height.toFloat()) * 0.3f

        val params = instance.customParams
        val offsetX = (params?.get("offsetX") as? Float) ?: 0f
        val offsetY = (params?.get("offsetY") as? Float) ?: 0f

        val headingRad = Math.toRadians(host.heading.toDouble())
        val cosH = cos(headingRad).toFloat()
        val sinH = sin(headingRad).toFloat()

        val cx = host.worldX + offsetX * cosH - offsetY * sinH
        val cy = host.worldY + offsetX * sinH + offsetY * cosH

        return floatArrayOf(cx - size, cy - size, cx + size, cy + size)
    }

    override fun process(toolParams: Map<String, Any>, instance: GpuEffectInstance): Map<String, Any> {
        return mapOf(
            "u_time" to instance.elapsed,
            "u_color1" to floatArrayOf(1f, 0.5f, 0f, 0.9f),
            "u_color2" to floatArrayOf(1f, 0.8f, 0f, 1f)
        )
    }
}