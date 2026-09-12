package com.rtnp.demo.gpu.effect.templates

import com.rtnp.demo.gpu.effect.EffectLayer
import com.rtnp.demo.gpu.effect.GpuEffectInstance
import com.rtnp.demo.gpu.effect.GpuEffectTemplate

class ScreenFlashEffect : GpuEffectTemplate {
    override val id = "screen_flash"
    override val shaderToolId = "flash"
    override val defaultDuration = 0.5f
    override val layer = EffectLayer.OVER_UNITS
    override val useWorldSpace = false  // ★ 屏幕空间，像素坐标
    override val followHost = false

    override fun process(toolParams: Map<String, Any>, instance: GpuEffectInstance): Map<String, Any> {
        val progress = instance.progress
        return mapOf(
            "u_progress" to progress,
            "u_radius" to 100f  // 像素半径，不随缩放变化
        )
    }
}