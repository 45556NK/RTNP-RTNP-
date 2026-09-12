package com.rtnp.demo.core

/**
 * 单位模板提供者接口
 * 实现此接口的类会被自动扫描并注册为单位模板
 */
interface UnitTemplateProvider {
    /** 返回该模板定义的 InventoryItem */
    fun provide(): InventoryItem
}