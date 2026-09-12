package com.rtnp.demo.ai

import com.rtnp.demo.core.PlacedObject

/**
 * AI 模板接口
 * 实现此接口的类会被自动扫描并注册
 */
interface AiTemplate {
    /** AI 唯一标识 */
    val id: String

    /** 显示名称 */
    val displayName: String

    /** 描述 */
    val description: String

    /** 默认是否启用 */
    val enabledByDefault: Boolean

    /** 每帧更新 */
    fun onUpdate(host: PlacedObject, deltaTime: Float, tools: AiTools)
}