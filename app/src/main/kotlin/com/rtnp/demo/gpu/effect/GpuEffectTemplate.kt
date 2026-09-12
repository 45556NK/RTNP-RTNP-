package com.rtnp.demo.gpu.effect

interface GpuEffectTemplate {
    val id: String
    val shaderToolId: String
    val defaultDuration: Float
    val layer: EffectLayer

    /** 是否跟随宿主单位移动 */
    val followHost: Boolean get() = false
    /** 是否绑定到宿主单位，由单位渲染器控制绘制（而非特效层全局绘制） */
    val followUnit: Boolean get() = false

    /** 是否使用世界空间（true：地图坐标，受相机缩放影响；false：屏幕坐标，固定像素大小） */
    val useWorldSpace: Boolean get() = true
    
    fun getWorldBounds(instance: GpuEffectInstance): FloatArray? = null

    fun process(toolParams: Map<String, Any>, instance: GpuEffectInstance): Map<String, Any>
    fun getFallbackFragmentShader(): String? = null
    fun getFallbackVertexShader(): String? = null
    fun isAlive(instance: GpuEffectInstance): Boolean {
        return instance.elapsed < defaultDuration
    }
}

enum class EffectLayer { UNDER_UNITS, OVER_UNITS }