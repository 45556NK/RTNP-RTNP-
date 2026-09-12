package com.rtnp.demo.data

import android.content.Context
import com.rtnp.demo.movement.weapon.WeaponManager
import com.rtnp.demo.movement.weapon.WeaponTemplate
import org.json.JSONObject
import java.io.IOException

object JsonWeaponLoader {

    fun loadFromAssets(context: Context) {
        try {
            val files = context.assets.list("data/weapons")
            if (files == null || files.isEmpty()) return
            for (fileName in files) {
                if (fileName.endsWith(".json")) {
                    loadWeaponFile(context, "data/weapons/$fileName")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadWeaponFile(context: Context, assetPath: String) {
        try {
            val jsonStr = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            val json = JSONObject(jsonStr)

            val type = json.optString("type", "")
            if (type != "weapon") return

            val template = parseWeapon(json)
            if (template != null) {
                WeaponManager.register(template)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseWeapon(json: JSONObject): WeaponTemplate? {
        val id = json.optString("id", "")
        if (id.isEmpty()) return null

        val category = json.optString("category", "railgun")

        val template = WeaponTemplate()
        template.id = id
        template.category = category
        template.subType = json.optString("subType", "")
        template.displayName = json.optString("displayName", category)
        template.range = json.optDouble("range", 200.0).toFloat()
        template.cooldown = json.optDouble("cooldown", 1.0).toFloat()
        template.active = json.optBoolean("active", true)
        template.damage = json.optDouble("damage", 20.0).toFloat()
        template.bulletSpeed = json.optDouble("bulletSpeed", 500.0).toFloat()
        template.minDamage = json.optDouble("minDamage", 50.0).toFloat()
        template.maxDamage = json.optDouble("maxDamage", 160.0).toFloat()
        template.chargeTime = json.optDouble("chargeTime", 10.0).toFloat()
        template.minRange = json.optDouble("minRange", 0.0).toFloat()
        template.fireEffectId = json.optString("fireEffectId", "")

        return template
    }
}