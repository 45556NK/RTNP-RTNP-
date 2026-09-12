package com.rtnp.demo.ai

import android.content.Context
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import java.util.*

object AiManager {

    private val templates = mutableMapOf<String, AiTemplate>()
    private var tools: AiTools? = null
    private var templatesLoaded = false

    /** 已启用 AI 的单位集合 */
    private val activeUnits = mutableMapOf<PlacedObject, MutableList<String>>()

    fun init(context: Context, unitSystem: UnitSystem) {
        tools = AiTools(unitSystem)
        if (!templatesLoaded) {
            // ★ 显式注册 AI 模板（不再使用 DexFile 扫描）
            AiTemplateRegistration.registerAiTemplates()
            templatesLoaded = true
        }
    }

    fun reinit(unitSystem: UnitSystem) {
        tools = AiTools(unitSystem)
        activeUnits.clear()
    }

    fun getTools(): AiTools? = tools

    // ==================== 单位 AI 控制 ====================

    /** 为单位添加 AI */
    fun addAi(unit: PlacedObject, aiId: String) {
        if (!templates.containsKey(aiId)) return
        val list = activeUnits.getOrPut(unit) { mutableListOf() }
        if (!list.contains(aiId)) {
            list.add(aiId)
        }
    }

    /** 移除单位的 AI */
    fun removeAi(unit: PlacedObject, aiId: String) {
        activeUnits[unit]?.remove(aiId)
    }

    /** 移除单位的所有 AI */
    fun removeAllAi(unit: PlacedObject) {
        activeUnits.remove(unit)
    }

    /** 检查单位是否拥有某个 AI */
    fun hasAi(unit: PlacedObject, aiId: String): Boolean {
        return activeUnits[unit]?.contains(aiId) == true
    }

    // ==================== 更新循环 ====================

    fun update(deltaTime: Float) {
        val t = tools ?: return
        val iterator = activeUnits.iterator()
        while (iterator.hasNext()) {
            val (unit, aiIds) = iterator.next()
            // 单位已死亡则移除
            if (unit.health <= 0) {
                iterator.remove()
                continue
            }
            for (id in aiIds) {
                templates[id]?.onUpdate(unit, deltaTime, t)
            }
        }
    }

    // ==================== 模板管理 ====================

    fun getTemplate(id: String): AiTemplate? = templates[id]
    fun getAllIds(): Set<String> = templates.keys

    /** 注册 AI 模板（供显式注册表调用） */
    fun register(template: AiTemplate) {
        templates[template.id] = template
    }
}