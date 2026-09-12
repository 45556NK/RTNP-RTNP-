package com.rtnp.demo.movement

import com.rtnp.demo.core.PlacedObject

class ItemManager private constructor() {

    class ItemDef {
        @JvmField var name: String = ""
        @JvmField var space: Int = 1
        @JvmField var mass: Int = 0
    }

    private val items = mutableMapOf<String, ItemDef>()

    init {
        registerItem("金属", 2, 4)
    }

    fun registerItem(name: String, space: Int) {
        registerItem(name, space, 0)
    }

    fun registerItem(name: String, space: Int, mass: Int) {
        val def = ItemDef()
        def.name = name
        def.space = space
        def.mass = mass
        items[name] = def
    }

    fun getItemDef(name: String): ItemDef? {
        return items[name]
    }

    // ==================== 矿物管理工具 ====================

    fun addMineral(unit: PlacedObject, itemName: String, count: Int) {
        if (unit.storedItemName == itemName) {
            unit.storedItemCount += count
        } else if (unit.storedItemName.isNullOrEmpty() || unit.storedItemCount <= 0) {
            unit.storedItemName = itemName
            unit.storedItemCount = count
        }
    }

    fun removeMineral(unit: PlacedObject, count: Int): Int {
        if (unit.storedItemName.isNullOrEmpty() || unit.storedItemCount <= 0) return 0
        val removed = minOf(count, unit.storedItemCount)
        unit.storedItemCount -= removed
        if (unit.storedItemCount <= 0) {
            unit.storedItemCount = 0
            unit.storedItemName = ""
        }
        return removed
    }

    fun clearMineral(unit: PlacedObject) {
        unit.storedItemCount = 0
        unit.storedItemName = ""
    }

    fun getTotalMass(unit: PlacedObject): Int {
        if (unit.storedItemName.isNullOrEmpty() || unit.storedItemCount <= 0) return 0
        val def = getItemDef(unit.storedItemName) ?: return 0
        return unit.storedItemCount * def.mass
    }

    companion object {
        @Volatile
        private var instance: ItemManager? = null

        @JvmStatic
        fun getInstance(): ItemManager {
            return instance ?: synchronized(this) {
                instance ?: ItemManager().also {
                    instance = it
                }
            }
        }
    }
}