package com.rtnp.demo.trait

import com.rtnp.demo.trait.templates.AntimatterBombTrait
import com.rtnp.demo.trait.templates.CarrierTrait
import com.rtnp.demo.trait.templates.FilterMissileTrait
import com.rtnp.demo.trait.templates.PredictiveAimTrait
import com.rtnp.demo.trait.templates.PulseDisableTrait
import com.rtnp.demo.trait.templates.SuppressionExplosionTrait

/**
 * 特性模板注册表（显式注册）
 *
 * 所有特性模板都在这里手动实例化并注册。
 * 不再使用 DexFile 扫描。
 */
object TraitTemplateRegistration {

    /**
     * 注册所有特性模板
     * 该方法应在 TraitManager.init() 中调用。
     */
    fun registerTraitTemplates() {
        TraitManager.register(AntimatterBombTrait())
        TraitManager.register(CarrierTrait())
        TraitManager.register(FilterMissileTrait())
        TraitManager.register(PredictiveAimTrait())
        TraitManager.register(PulseDisableTrait())
        TraitManager.register(SuppressionExplosionTrait())
    }
}