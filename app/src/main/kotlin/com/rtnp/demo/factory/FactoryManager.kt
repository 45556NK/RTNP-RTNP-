package com.rtnp.demo.factory

import android.content.Context
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject

/**
 * 工厂槽管理器（单例）。
 * 负责注册模板、更新工厂槽逻辑、处理点击。
 */
object FactoryManager {

    private val templates = mutableMapOf<String, FactoryTemplate>()
    private var tools: FactoryTools? = null
    private var templatesLoaded = false

    @JvmStatic
    fun init(context: Context, unitSystem: UnitSystem) {
        tools = FactoryTools(unitSystem)
        if (!templatesLoaded) {
            FactoryTemplateRegistration.registerFactoryTemplates()
            templatesLoaded = true
        }
    }

    @JvmStatic
    fun reinit(unitSystem: UnitSystem) {
        tools = FactoryTools(unitSystem)
    }

    @JvmStatic
    fun getTools(): FactoryTools? = tools

    @JvmStatic
    fun register(template: FactoryTemplate) {
        templates[template.id] = template
    }

    @JvmStatic
    fun getTemplate(id: String): FactoryTemplate? = templates[id]

    @JvmStatic
    fun getAllIds(): Set<String> = templates.keys

    /**
     * 创建槽位参数：先填充模板默认值，再用外部传入值覆盖
     */
    @JvmStatic
    fun createSlotData(factoryId: String, overrideData: FactorySlotData? = null): FactorySlotData {
        val data = FactorySlotData()
        templates[factoryId]?.defaultParams?.forEach { (key, value) ->
            data.put(key, value)
        }
        overrideData?.getAll()?.forEach { (key, value) ->
            data.put(key, value)
        }
        return data
    }

    /**
     * 每帧更新所有工厂槽
     */
    @JvmStatic
    fun updateFactories(unit: PlacedObject, deltaTime: Float) {
        val t = tools ?: return

        for (i in unit.factorySlotConfigs.indices) {
            val config = unit.factorySlotConfigs[i]
            val template = templates[config.factoryId] ?: continue

            template.onUpdate(unit, deltaTime, t, config.params)

            if (template.cooldownMode == com.rtnp.demo.strategy.CooldownMode.CLICK && template.cooldown > 0f) {
                val bar = unit.getOrCreateFactoryCooldownBar(i, template.cooldown)
                if (!bar.isFinished) {
                    bar.addProgress(deltaTime / bar.totalDuration)
                }
            }
        }
    }

    /**
     * 玩家点击工厂槽
     */
    @JvmStatic
    fun onClickFactory(unit: PlacedObject, slotIndex: Int) {
        val t = tools ?: return
        if (slotIndex < 0 || slotIndex >= unit.factorySlotConfigs.size) return

        val config = unit.factorySlotConfigs[slotIndex]
        val template = templates[config.factoryId] ?: return

        if (template.cooldown > 0f && slotIndex < unit.factoryCooldownBars.size) {
            val bar = unit.factoryCooldownBars[slotIndex]
            if (bar != null && !bar.isFinished) return
        }

        if (template.cooldownMode == com.rtnp.demo.strategy.CooldownMode.CLICK && template.cooldown > 0f) {
            val bar = unit.getOrCreateFactoryCooldownBar(slotIndex, template.cooldown)
            bar.reset()
        }

        template.onClicked(unit, t, slotIndex, config.params)
    }

    /**
     * 外部修改指定槽位的参数
     */
    @JvmStatic
    fun updateSlotParam(unit: PlacedObject, slotIndex: Int, key: String, value: Any) {
        if (slotIndex < 0 || slotIndex >= unit.factorySlotConfigs.size) return
        unit.factorySlotConfigs[slotIndex].params.put(key, value)
    }

    /**
     * 外部获取指定槽位的参数容器
     */
    @JvmStatic
    fun getSlotData(unit: PlacedObject, slotIndex: Int): FactorySlotData? {
        if (slotIndex < 0 || slotIndex >= unit.factorySlotConfigs.size) return null
        return unit.factorySlotConfigs[slotIndex].params
    }
}