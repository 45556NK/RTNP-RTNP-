package com.rtnp.demo.core

import com.rtnp.demo.data.units.B1CarrierRocketTemplate
import com.rtnp.demo.data.units.ClusterRocketTemplate
import com.rtnp.demo.data.units.DriftingBeaconTemplate
import com.rtnp.demo.data.units.PulseTowerTemplate
import com.rtnp.demo.data.units.RepairShipTemplate
import com.rtnp.demo.data.units.TeleportAnchorTemplate

/**
 * 单位模板注册表（显式注册）
 *
 * 所有通过 Kotlin 类定义的普通单位模板都在这里手动实例化并注册。
 * 不再使用 DexFile 扫描。
 *
 * 注意：导弹类型注册见 MissileTemplateRegistration。
 */
object UnitTemplateRegistration {

    /**
     * 注册所有普通单位模板（来自 data.units 包）
     * 该方法应在 TemplateRegistry.init() 中调用。
     */
    fun registerNormalUnitTemplates(registry: TemplateRegistry) {
        // B1运载火箭
        registry.addTemplate(B1CarrierRocketTemplate().provide())
        // 集群火箭
        registry.addTemplate(ClusterRocketTemplate().provide())
        // 漂泊信标
        registry.addTemplate(DriftingBeaconTemplate().provide())
        // 脉冲塔
        registry.addTemplate(PulseTowerTemplate().provide())
        // 尘埃重组无人机
        registry.addTemplate(RepairShipTemplate().provide())
        // 传送锚点
        registry.addTemplate(TeleportAnchorTemplate().provide())
    }
}