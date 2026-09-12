// app/src/main/kotlin/com/rtnp/demo/data/missiles/HeavyMissileTemplate.kt
package com.rtnp.demo.data.missiles

import android.graphics.Color
import com.rtnp.demo.core.MissileTemplateProvider
import com.rtnp.demo.core.MissileType

class HeavyMissileTemplate : MissileTemplateProvider {
    override fun provide(): MissileType {
        return MissileType().apply {
            name = "反物质导弹"
            health = 600
            damage = 2000
            explosionRange = 200
            startSpeed = 10
            maxSpeed = 120
            acceleration = 20f
            lifetime = 12
            effectSize = 80
            cooldownMax = 8
            minRange = 180
            size = 40
            shape = "square"
            color = Color.GRAY
            hasSuppression = true
            suppressionRange = 80
            suppressionDamage = 300
            suppressionEffectId = "explosion"  // 使用压制爆炸特效
        }
    }
}