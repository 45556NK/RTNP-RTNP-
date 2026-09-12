package com.rtnp.demo.factory

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.TemplateRegistry

/**
 * 工厂槽工具集，提供给工厂模板使用。
 */
class FactoryTools(private val unitSystem: UnitSystem) {

    fun getAllUnits(): List<PlacedObject> = unitSystem.placedObjects.toList()

    fun getUnitsByFaction(faction: Int): List<PlacedObject> =
        unitSystem.placedObjects.filter { it.faction == faction }

    fun getHealth(unit: PlacedObject): Float = unitSystem.healthMap[unit] ?: 0f

    fun setHealth(unit: PlacedObject, health: Float) {
        unitSystem.healthMap[unit] = health
        unit.health = health.toInt()
    }

    fun spawnUnit(templateName: String, worldX: Float, worldY: Float): PlacedObject? {
        val template = TemplateRegistry.templates.firstOrNull { it.name == templateName } ?: return null
        unitSystem.addObjectFromItem(template, worldX, worldY)
        return unitSystem.placedObjects.lastOrNull()
    }

    fun spawnUnit(item: InventoryItem, worldX: Float, worldY: Float): PlacedObject? {
        unitSystem.addObjectFromItem(item, worldX, worldY)
        return unitSystem.placedObjects.lastOrNull()
    }

    fun removeUnit(unit: PlacedObject) {
        unitSystem.placedObjects.remove(unit)
        unitSystem.healthMap.remove(unit)
        unitSystem.unitsWithLifetime.remove(unit)
        if (unit == unitSystem.selectedUnit) {
            unitSystem.deselectUnit()
        }
    }
}