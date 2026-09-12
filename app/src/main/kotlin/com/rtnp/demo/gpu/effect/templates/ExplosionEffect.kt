package com.rtnp.demo.gpu.effect.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.effect.EffectLayer
import com.rtnp.demo.gpu.effect.GpuEffectInstance
import com.rtnp.demo.gpu.effect.GpuEffectTemplate

class ExplosionEffect : GpuEffectTemplate {
    override val id = "explosion"
    override val shaderToolId = "explosion_new"
    override val defaultDuration = 0.8f
    override val layer = EffectLayer.OVER_UNITS
    override val useWorldSpace = true
    override val followHost = false

    override fun getWorldBounds(instance: GpuEffectInstance): FloatArray {
        // 从宿主导弹获取爆炸范围，乘以3
        val radius = (instance.hostUnit as? PlacedObject)?.explosionRange?.toFloat()?.times(2f) ?: 300f
        return floatArrayOf(
            instance.worldX - radius, instance.worldY - radius,
            instance.worldX + radius, instance.worldY + radius
        )
    }

    override fun process(toolParams: Map<String, Any>, instance: GpuEffectInstance): Map<String, Any> {
        return mapOf("u_progress" to instance.progress)
    }
}