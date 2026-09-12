package com.rtnp.demo.core

import com.rtnp.demo.data.missiles.ClusterMissileTemplate
import com.rtnp.demo.data.missiles.HeavyMissileTemplate

/**
 * 导弹类型模板注册表（显式注册）
 *
 * 所有通过 Kotlin 类定义的导弹类型都在这里手动实例化并注册。
 * 不再使用 DexFile 扫描。
 */
object MissileTemplateRegistration {

    /**
     * 注册所有导弹类型模板
     * 该方法应在 TemplateRegistry.init() 中调用。
     */
    fun registerMissileTemplates() {
        // 集群导弹
        MissileTypeRegistry.register(ClusterMissileTemplate().provide())
        // 反物质导弹
        MissileTypeRegistry.register(HeavyMissileTemplate().provide())
    }
}