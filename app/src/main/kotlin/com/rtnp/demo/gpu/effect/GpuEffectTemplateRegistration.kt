package com.rtnp.demo.gpu.effect

import com.rtnp.demo.gpu.effect.templates.BulletEffect
import com.rtnp.demo.gpu.effect.templates.ExplosionEffect
import com.rtnp.demo.gpu.effect.templates.ScreenFlashEffect
import com.rtnp.demo.gpu.effect.templates.SuppressionExplosionEffect
import com.rtnp.demo.gpu.effect.templates.TailFlameEffect

/**
 * GPU 特效模板注册表（显式注册）
 *
 * 所有 GPU 特效模板都在这里手动实例化并注册。
 * 不再使用 DexFile 扫描。
 */
object GpuEffectTemplateRegistration {

    /**
     * 注册所有 GPU 特效模板
     * 该方法应在 GpuEffectSystem.init() 中调用。
     */
    fun registerEffectTemplates() {
        GpuEffectSystem.registerTemplate(ExplosionEffect())
        GpuEffectSystem.registerTemplate(BulletEffect())
        GpuEffectSystem.registerTemplate(ScreenFlashEffect())
        GpuEffectSystem.registerTemplate(SuppressionExplosionEffect())
        GpuEffectSystem.registerTemplate(TailFlameEffect())
    }
}