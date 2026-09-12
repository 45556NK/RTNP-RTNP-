// app/src/main/kotlin/com/rtnp/demo/gpu/effect/templates/SuppressionExplosionEffect.kt
package com.rtnp.demo.gpu.effect.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.effect.EffectLayer
import com.rtnp.demo.gpu.effect.GpuEffectInstance
import com.rtnp.demo.gpu.effect.GpuEffectTemplate

class SuppressionExplosionEffect : GpuEffectTemplate {
    override val id = "suppression_explosion"
    override val shaderToolId = "suppression_explosion"
    override val defaultDuration = 0.4f
    override val layer = EffectLayer.OVER_UNITS
    override val useWorldSpace = true
    override val followHost = false

    @JvmField var defaultRange: Float = 200f

    override fun getWorldBounds(instance: GpuEffectInstance): FloatArray {
        val host = instance.hostUnit as? PlacedObject
        val baseRange = if (host != null && host.suppressionRange > 0) {
            host.suppressionRange.toFloat()
        } else {
            defaultRange
        }
        val radius = baseRange * 1.5f
        return floatArrayOf(
            instance.worldX - radius, instance.worldY - radius,
            instance.worldX + radius, instance.worldY + radius
        )
    }

    override fun process(toolParams: Map<String, Any>, instance: GpuEffectInstance): Map<String, Any> {
        val seed = kotlin.random.Random.nextInt(24).toFloat()
        return mapOf(
            "u_progress" to instance.progress,
            "u_random_seed" to seed,
            "u_center" to floatArrayOf(0.5f, 0.5f),
            "u_aspect" to 1.0f
        )
    }
}