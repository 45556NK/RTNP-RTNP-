package com.rtnp.demo.core

import java.util.*

object MissileTypeRegistry {

    private val registry = mutableMapOf<String, MissileType>()

    @JvmStatic
    fun register(type: MissileType) {
        registry[type.name] = type
    }

    @JvmStatic
    fun get(name: String): MissileType? = registry[name]
}