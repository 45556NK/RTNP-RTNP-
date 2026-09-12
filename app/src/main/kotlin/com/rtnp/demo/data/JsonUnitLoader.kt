package com.rtnp.demo.data

import android.content.Context
import android.graphics.Color
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.movement.ModuleManager
import com.rtnp.demo.ui.InventoryPanel
import org.json.JSONObject
import java.io.IOException

object JsonUnitLoader {

    fun loadFromAssets(context: Context) {
        try {
            val files = context.assets.list("data/units")
            if (files == null || files.isEmpty()) return
            for (fileName in files) {
                if (fileName.endsWith(".json")) {
                    loadUnitFile(context, "data/units/$fileName")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadUnitFile(context: Context, assetPath: String) {
        try {
            val jsonStr = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            val json = JSONObject(jsonStr)

            val type = json.optString("type", "")
            if (type != "unit") return

            val item = parseUnit(json)
            if (item != null) {
                InventoryPanel.registerTemplate(item)
                TemplateRegistry.addTemplate(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseUnit(json: JSONObject): InventoryItem? {
        val name = json.optString("name", "")
        if (name.isEmpty()) return null

        val item = InventoryItem()
        item.name = name
        item.type = "unit"
        item.shape = json.optString("shape", "rectangle")
        item.size = json.optInt("size", 50)
        item.width = json.optInt("width", 90)
        item.height = json.optInt("height", 30)
        item.health = json.optInt("health", 1000)
        item.speed = json.optInt("speed", 80)
        item.acceleration = json.optDouble("acceleration", 20.0).toFloat()
        item.maxSpeed = json.optDouble("maxSpeed", 160.0).toFloat()
        item.deceleration = json.optDouble("deceleration", 40.0).toFloat()
        item.turnRate = json.optDouble("turnRate", 180.0).toFloat()
        item.faction = json.optInt("faction", 2)
        item.category = json.optString("category", "")
        item.color = parseColor(json.optString("color", "#FFFFFF"), Color.WHITE)
        item.description = json.optString("description", "")
        item.autoControl = json.optBoolean("autoControl", false)
        item.useTexture = json.optBoolean("useTexture", false)
        item.texturePath = json.optString("texturePath", null)
        item.lifetime = json.optDouble("lifetime", 0.0).toFloat()
        item.textureDisplayHeight = json.optDouble("textureDisplayHeight", 0.0).toFloat()
        item.allowWeaponChange = json.optBoolean("allowWeaponChange", true)
        item.allowStrategyChange = json.optBoolean("allowStrategyChange", true)

        val weaponsArray = json.optJSONArray("weaponSlots")
        if (weaponsArray != null) {
            for (i in 0 until weaponsArray.length()) {
                val wsJson = weaponsArray.getJSONObject(i)
                val templateId = wsJson.optString("templateId", "")
                if (templateId.isNotEmpty()) {
                    val slot = ModuleManager.createSlotFromTemplate(templateId)
                    if (slot != null) {
                        item.weaponSlots.add(slot)
                    }
                }
            }
        } else {
            item.weaponType = json.optString("weaponType", "?")
            item.range = json.optDouble("range", 0.0).toFloat()
            item.damage = json.optDouble("damage", 0.0).toFloat()
            item.bulletSpeed = json.optDouble("bulletSpeed", 0.0).toFloat()
            item.chargeTime = json.optDouble("chargeTime", 0.0).toFloat()
            item.minDamage = json.optDouble("minDamage", 0.0).toFloat()
            item.maxDamage = json.optDouble("maxDamage", 0.0).toFloat()
        }

        if (!json.isNull("physicsMass") && json.has("physicsMass")) {
            val massValue = json.optDouble("physicsMass", Double.NaN)
            item.physicsMass = if (massValue.isNaN()) null else massValue.toFloat()
        } else {
            item.physicsMass = null
        }

        item.missileTypeName = json.optString("missileTypeName", "")

        item.maxDockingSlots = json.optInt("maxDockingSlots", 6)
        item.dockRadius = json.optDouble("dockRadius", 60.0).toFloat()

        item.storedItemName = json.optString("storedItemName", null)
        item.storedItemCount = json.optInt("storedItemCount", 0)
        item.miningSpeed = json.optDouble("miningSpeed", 0.0).toFloat()
        item.storageCapacity = json.optInt("storageCapacity", 0)
        item.maxMiners = json.optInt("maxMiners", 0)
        item.costItem = json.optString("costItem", null)
        item.costAmount = json.optInt("costAmount", 0)

        item.effectId = json.optString("effectId", "")
        item.effectOffsetX = json.optDouble("effectOffsetX", 0.0).toFloat()
        item.effectOffsetY = json.optDouble("effectOffsetY", 0.0).toFloat()
        item.effectAngleOffset = json.optDouble("effectAngleOffset", 180.0).toFloat()
        item.effectTriggerSpeed = json.optDouble("effectTriggerSpeed", 20.0).toFloat()

        if (json.has("refVolume")) {
            val rv = json.getJSONObject("refVolume")
            item.refVolumeShape = rv.optString("shape", "")
            item.refVolumeRadius = rv.optDouble("radius", 0.0).toFloat()
        }

        val traitArray = json.optJSONArray("traitIds")
        if (traitArray != null) {
            for (i in 0 until traitArray.length()) {
                item.traitIds.add(traitArray.getString(i))
            }
        }
        item.strategySlots = json.optInt("strategySlots", 0)
        val strategyArray = json.optJSONArray("strategyIds")
        if (strategyArray != null) {
            for (i in 0 until strategyArray.length()) {
                item.strategyIds.add(strategyArray.getString(i))
            }
        }

        item.buildAmount = json.optDouble("buildAmount", 0.0).toFloat()
        item.buildSpeed = json.optDouble("buildSpeed", 0.0).toFloat()
        item.buildDockSlots = json.optInt("buildDockSlots", 0)
        item.buildDockRadius = json.optDouble("buildDockRadius", 0.0).toFloat()

        return item
    }

    private fun parseColor(colorStr: String, defaultColor: Int): Int {
        return try {
            if (colorStr.startsWith("#")) {
                Color.parseColor(colorStr)
            } else {
                when (colorStr.lowercase()) {
                    "red" -> Color.RED
                    "green" -> Color.GREEN
                    "blue" -> Color.BLUE
                    "yellow" -> Color.YELLOW
                    "cyan" -> Color.CYAN
                    "magenta" -> Color.MAGENTA
                    "white" -> Color.WHITE
                    "black" -> Color.BLACK
                    "gray", "grey" -> Color.GRAY
                    else -> defaultColor
                }
            }
        } catch (e: Exception) {
            defaultColor
        }
    }
}