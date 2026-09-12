package com.rtnp.demo.ai

import com.rtnp.demo.ai.automation.AutoMineAI
import com.rtnp.demo.ai.automation.ZoneDefense
import com.rtnp.demo.ai.automation.EngineerAutoDeployAI

/**
 * AI 模板注册表（显式注册）
 *
 * 所有 AI 模板都在这里手动实例化并注册。
 * 不再使用 DexFile 扫描。
 */
object AiTemplateRegistration {

    /**
     * 注册所有 AI 模板
     * 该方法应在 AiManager.init() 中调用。
     */
    fun registerAiTemplates() {
        // 自动采矿
        AiManager.register(AutoMineAI())
        // 区域防守
        AiManager.register(ZoneDefense())
        
        AiManager.register(EngineerAutoDeployAI)

    }
}