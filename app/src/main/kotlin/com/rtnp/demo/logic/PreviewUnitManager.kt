// app/src/main/kotlin/com/rtnp/demo/logic/PreviewUnitManager.kt
package com.rtnp.demo.logic

import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.TempUnitData

object PreviewUnitManager {
    private val units = mutableListOf<TempUnitData>()

    fun add(unit: TempUnitData) {
        units.add(unit)
    }

    fun add(x: Float, y: Float, item: InventoryItem) {
        units.add(TempUnitData(x, y, item))
    }

    fun remove(unit: TempUnitData) {
        units.remove(unit)
    }

    fun clear() {
        units.clear()
    }

    fun isEmpty(): Boolean = units.isEmpty()

    fun getUnits(): List<TempUnitData> = units.toList()
}