package com.rtnp.demo.gpu.effect

import com.rtnp.demo.core.PlacedObject

class GpuEffectInstance(
    val template: GpuEffectTemplate,
    var worldX: Float,
    var worldY: Float,
    var rotation: Float = 0f,
    var scale: Float = 1f,
    var elapsed: Float = 0f,
    var hostUnit: PlacedObject? = null,
    val customParams: Map<String, Any>? = null,
    var offsetToHostX: Float = 0f,
    var offsetToHostY: Float = 0f
) {
    val isExpired: Boolean get() = !template.isAlive(this)
    val progress: Float get() = (elapsed / template.defaultDuration).coerceIn(0f, 1f)

    fun update(deltaTime: Float) {
        elapsed += deltaTime
    }
}