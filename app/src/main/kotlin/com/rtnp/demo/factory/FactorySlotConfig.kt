package com.rtnp.demo.factory

/**
 * 单个工厂槽的完整配置
 */
class FactorySlotConfig {
    @JvmField var factoryId: String = ""
    @JvmField var params: FactorySlotData = FactorySlotData()
    @JvmField var iconPath: String? = null  // 图标路径（UI用，暂不处理）

    fun copy(): FactorySlotConfig {
        val newConfig = FactorySlotConfig()
        newConfig.factoryId = factoryId
        newConfig.params = params.copy()
        newConfig.iconPath = iconPath
        return newConfig
    }
}