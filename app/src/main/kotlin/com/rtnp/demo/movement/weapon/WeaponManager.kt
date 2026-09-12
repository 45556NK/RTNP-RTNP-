package com.rtnp.demo.movement.weapon

object WeaponManager {

    private val templates = mutableMapOf<String, WeaponTemplate>()

    @JvmStatic
    fun getTemplate(id: String): WeaponTemplate? = templates[id]

    @JvmStatic
    fun getAllTemplates(): Map<String, WeaponTemplate> = templates

    @JvmStatic
    fun register(template: WeaponTemplate) {
        if (!templates.containsKey(template.id)) {
            templates[template.id] = template
        }
    }

    private fun registerInternal(template: WeaponTemplate) {
        templates[template.id] = template
    }

    init {
        registerInternal(createRailgunA1())
        registerInternal(createRailgunA2())
        registerInternal(createRailgunB1())
        registerInternal(createRailgunB2())
        registerInternal(createLaserDefault())
        registerInternal(createMissileDefault())
        registerInternal(createPulseDefault())
        registerInternal(createPulseDefaultX1())
    }

    private fun createPulseDefault(): WeaponTemplate {
        return WeaponTemplate().apply {
            id = "pulse_default"
            category = "pulse"
            subType = "A1"
            displayName = "脉冲"
            range = 400f
            cooldown = 3f
            damage = 600f
            maxTargets = 3
            energyDuration = 8f
            energyRegenDelay = 7f   // ★ 3秒后开始恢复
            energyRegenSpeed = 0.1f // ★ 约3.3秒回满
            traitIds.add("filter_missile")  // ★ 添加这行
            filterWhitelist["filter_missile"] = mutableListOf("标准导弹")
        }
    }
    
    private fun createPulseDefaultX1(): WeaponTemplate {
        return WeaponTemplate().apply {
            id = "pulse_default_x1"
            category = "pulse"
            subType = "X1"
            displayName = "脉冲"
            range = 600f
            cooldown = 3f
            damage = 720f
            maxTargets = 4
            energyDuration = 8f
            energyRegenDelay = 3f   // ★ 3秒后开始恢复
            energyRegenSpeed = 0.3f // ★ 约3.3秒回满
            traitIds.add("filter_missile")  // ★ 添加这行
        }
    }

    private fun createRailgunA1(): WeaponTemplate {
        return WeaponTemplate().apply {
            id = "railgun_a1"
            category = "railgun"
            subType = "A1"
            displayName = "轨道炮"
            range = 460f
            cooldown = 0.2f
            damage = 80f
            bulletSpeed = 800f
        }
    }

    private fun createRailgunA2(): WeaponTemplate {
        return WeaponTemplate().apply {
            id = "railgun_a2"
            category = "railgun"
            subType = "A2"
            displayName = "轨道炮"
            range = 730f
            cooldown = 2.2f
            damage = 375f
            bulletSpeed = 1000f
            iconPath = "images/icons/weapon_railgun_a2.png"
        }
    }

    private fun createRailgunB1(): WeaponTemplate {
        return WeaponTemplate().apply {
            id = "railgun_b1"
            category = "railgun"
            subType = "B1"
            displayName = "轨道炮"
            range = 800f
            cooldown = 2.0f
            damage = 235f
            bulletSpeed = 1500f
        }
    }

    private fun createRailgunB2(): WeaponTemplate {
        return WeaponTemplate().apply {
            id = "railgun_b2"
            category = "railgun"
            subType = "B2"
            displayName = "轨道炮"
            range = 800f
            cooldown = 0.05f
            damage = 12f
            bulletSpeed = 1500f
        }
    }

    private fun createLaserDefault(): WeaponTemplate {
        return WeaponTemplate().apply {
            id = "laser_default"
            category = "laser"
            subType = "A1"
            displayName = "激光"
            range = 460f
            cooldown = 0f
            chargeTime = 10f
            minDamage = 180f
            maxDamage = 270f
            fireEffectId = "laser_spark"
        }
    }

    private fun createMissileDefault(): WeaponTemplate {
        return WeaponTemplate().apply {
            id = "missile_default"
            category = "missile"
            subType = "A1"
            displayName = "导弹"
            range = 1200f
            cooldown = 8f
            minRange = 180f
            missileTypeName = "集群导弹"
        }
    }
}