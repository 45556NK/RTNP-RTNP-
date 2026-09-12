// app/src/main/kotlin/com/rtnp/demo/logic/LifetimeSystem.kt
package com.rtnp.demo.logic

import com.rtnp.demo.core.PlacedObject

object LifetimeSystem {

    /**
     * 更新单位寿命，并返回是否已过期。
     */
    fun updateAndCheck(entity: PlacedObject, deltaTime: Float, maxLifetime: Float): Boolean {
        entity.lifetime += deltaTime
        return entity.lifetime >= maxLifetime
    }

    fun isExpired(entity: PlacedObject, maxLifetime: Float): Boolean {
        return entity.lifetime >= maxLifetime
    }
}