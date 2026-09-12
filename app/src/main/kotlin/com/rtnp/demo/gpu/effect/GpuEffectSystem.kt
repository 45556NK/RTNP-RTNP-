package com.rtnp.demo.gpu.effect

import android.content.Context
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.logger.Logger

object GpuEffectSystem {

    private var underLayer: GpuEffectLayer? = null
    private var overLayer: GpuEffectLayer? = null
    private val templates = mutableMapOf<String, GpuEffectTemplate>()
    private var initialized = false
    private val pendingSpawns = mutableListOf<SpawnRequest>()

    data class SpawnRequest(
        val templateId: String,
        val worldX: Float,
        val worldY: Float,
        val host: PlacedObject?,
        val params: Map<String, Any>?
    )

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        ShaderToolRegistry.init(context)
        underLayer = GpuEffectLayer(EffectLayer.UNDER_UNITS)
        overLayer = GpuEffectLayer(EffectLayer.OVER_UNITS)

        // ★ 显式注册 GPU 特效模板（不再使用 DexFile 扫描）
        GpuEffectTemplateRegistration.registerEffectTemplates()

        for (req in pendingSpawns) {
            spawnInternal(req.templateId, req.worldX, req.worldY, req.host, req.params)
        }
        pendingSpawns.clear()
    }

    fun spawn(templateId: String, worldX: Float, worldY: Float, host: PlacedObject? = null, params: Map<String, Any>? = null) {
        if (!initialized) {
            pendingSpawns.add(SpawnRequest(templateId, worldX, worldY, host, params))
            return
        }
        spawnInternal(templateId, worldX, worldY, host, params)
    }

    private fun spawnInternal(templateId: String, worldX: Float, worldY: Float, host: PlacedObject?, params: Map<String, Any>?) {
        val template = templates[templateId] ?: run {
            Logger.w("GpuEffectSystem", "模板未找到: $templateId")
            return
        }
        val instance = GpuEffectInstance(
            template = template,
            worldX = worldX,
            worldY = worldY,
            hostUnit = host,
            customParams = params,
            offsetToHostX = if (host != null) worldX - host.worldX else 0f,
            offsetToHostY = if (host != null) worldY - host.worldY else 0f
        )
        when (template.layer) {
            EffectLayer.UNDER_UNITS -> underLayer?.addInstance(instance)
            EffectLayer.OVER_UNITS -> overLayer?.addInstance(instance)
        }
    }

    fun update(deltaTime: Float) {
        if (!initialized) return
        underLayer?.update(deltaTime)
        overLayer?.update(deltaTime)
    }

    fun drawUnder(cameraMatrix: CameraMatrix) {
        if (!initialized) return
        underLayer?.draw(cameraMatrix)
    }

    fun drawOver(cameraMatrix: CameraMatrix) {
        if (!initialized) return
        overLayer?.draw(cameraMatrix)
    }

    /** 注册 GPU 特效模板（供显式注册表调用） */
    fun registerTemplate(template: GpuEffectTemplate) {
        templates[template.id] = template
        Logger.d("GpuEffectSystem", "模板注册成功: ${template.id}")
    }

    /**
     * 获取所有绑定到指定单位的特效实例（followUnit = true）
     */
    fun getUnitEffects(unit: PlacedObject): List<GpuEffectInstance> {
        val result = mutableListOf<GpuEffectInstance>()
        underLayer?.instances?.filter { it.hostUnit == unit && it.template.followUnit }?.let { result.addAll(it) }
        overLayer?.instances?.filter { it.hostUnit == unit && it.template.followUnit }?.let { result.addAll(it) }
        return result
    }

    fun drawInstance(instance: GpuEffectInstance, cameraMatrix: CameraMatrix) {
        when (instance.template.layer) {
            EffectLayer.UNDER_UNITS -> underLayer?.drawInstance(instance, cameraMatrix)
            EffectLayer.OVER_UNITS -> overLayer?.drawInstance(instance, cameraMatrix)
        }
    }

    fun getTemplate(id: String): GpuEffectTemplate? = templates[id]
}