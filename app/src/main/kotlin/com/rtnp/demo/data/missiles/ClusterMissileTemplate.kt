// app/src/main/kotlin/com/rtnp/demo/data/missiles/ClusterMissileTemplate.kt
package com.rtnp.demo.data.missiles

import android.graphics.Color
import com.rtnp.demo.core.MissileTemplateProvider
import com.rtnp.demo.core.MissileType

class ClusterMissileTemplate : MissileTemplateProvider {
    override fun provide(): MissileType {
        return MissileType().apply {
            name = "集群导弹"
            health = 200
            damage = 1000
            startSpeed = 10
            maxSpeed = 360
            acceleration = 80f
            lifetime = 12
            explosionRange = 400
            effectSize = 100
            cooldownMax = 10
            minRange = 100
            size = 30
            shape = "rectangle"
            color = Color.rgb(255, 100, 50)
        }
    }
}