package com.rtnp.demo.gpu.effect.templates

import com.rtnp.demo.gpu.effect.EffectLayer
import com.rtnp.demo.gpu.effect.GpuEffectInstance
import com.rtnp.demo.gpu.effect.GpuEffectTemplate
import kotlin.math.cos
import kotlin.math.sin

class BulletEffect : GpuEffectTemplate {
    override val id = "bullet"
    override val shaderToolId = "bullet"
    override val defaultDuration = 0.05f   // 子弹特效持续时间很短
    override val layer = EffectLayer.UNDER_UNITS
    override val useWorldSpace = true
    override val followHost = true          // 跟随子弹移动

    override fun getWorldBounds(instance: GpuEffectInstance): FloatArray {
        val radius = 30f // 世界单位包围盒
        return floatArrayOf(
            instance.worldX - radius, instance.worldY - radius,
            instance.worldX + radius, instance.worldY + radius
        )
    }

    override fun process(toolParams: Map<String, Any>, instance: GpuEffectInstance): Map<String, Any> {
        // 从额外数据中获取飞行方向
        val angle = (toolParams["angle"] as? Float) ?: 0f
        val dirX = cos(Math.toRadians(angle.toDouble())).toFloat()
        val dirY = sin(Math.toRadians(angle.toDouble())).toFloat()
        return mapOf(
            "u_progress" to instance.progress,
            "u_direction" to floatArrayOf(dirX, dirY)
        )
    }
}