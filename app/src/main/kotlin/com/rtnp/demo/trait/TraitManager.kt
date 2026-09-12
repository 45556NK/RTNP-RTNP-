package com.rtnp.demo.trait

import android.content.Context
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.TemplateRegistry
import java.util.*

object TraitManager {

    private val templates = mutableMapOf<String, TraitTemplate>()
    val tools = TraitTools()
    private var initialized = false

    private var lastFrameAliveTraitUnits = setOf<Long>()
    private val unitTraitDataMap = mutableMapOf<Long, TraitUnitData>()
    private val globalInstances = mutableListOf<GlobalTraitInstance>()
    private var unitSystem: UnitSystem? = null

    private data class TraitUnitData(
        val traitIds: List<String>,
        val worldX: Float,
        val worldY: Float,
        val targetX: Float,
        val targetY: Float,
        val missileHealth: Int = 0,
        val suppressionRangeDisplayId: Long = -1L,
        val suppressionEffectId: String? = null
    )

    data class GlobalTraitInstance(
        val traitId: String,
        val worldX: Float,
        val worldY: Float,
        val targetX: Float,
        val targetY: Float,
        val missileHealth: Int,
        val displayId: Long = -1L,
        val suppressionEffectId: String? = null
    )

    fun init(context: Context) {
        if (initialized) return
        initialized = true

        // ★ 显式注册特性模板（不再使用 DexFile 扫描）
        TraitTemplateRegistration.registerTraitTemplates()

        // 可选：从 assets/traits/*.json 加载额外特性（保留原逻辑，但当前 JSON 仅包含类名，可注释掉）
        // loadFromAssets(context)
    }

    fun setUnitSystem(us: UnitSystem) {
        this.unitSystem = us
        tools.removeUnitCallback = { unit ->
            us.placedObjects.remove(unit)
            us.healthMap.remove(unit)
            us.unitsWithLifetime.remove(unit)
            if (unit == us.selectedUnit) us.deselectUnit()
        }
        tools.spawnUnitCallback = { name, x, y ->
            val template = TemplateRegistry.templates.firstOrNull { it.name == name }
            if (template != null) {
                us.addObjectFromItem(template, x, y)
                us.placedObjects.lastOrNull()
            } else null
        }
    }

    private fun loadFromAssets(context: Context) {
        try {
            val files = context.assets.list("traits")
            if (files == null || files.isEmpty()) return
            for (fileName in files) {
                if (!fileName.endsWith(".json")) continue
                loadTraitFile(context, "traits/$fileName")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadTraitFile(context: Context, path: String) {
        try {
            val content = context.assets.open(path).bufferedReader().use { it.readText() }
            val json = org.json.JSONObject(content)
            if (!json.has("className") || !json.has("id")) return
            val className = json.getString("className")
            registerByClassName(className)
        } catch (e: Exception) {
        }
    }

    private fun registerByClassName(className: String) {
        try {
            val clazz = Class.forName(className)
            if (!TraitTemplate::class.java.isAssignableFrom(clazz)) return
            if (java.lang.reflect.Modifier.isAbstract(clazz.modifiers)) return
            val instance = clazz.getDeclaredConstructor().newInstance()
            if (instance is TraitTemplate) {
                register(instance)
            }
        } catch (e: ClassNotFoundException) {
        } catch (e: InstantiationException) {
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun register(template: TraitTemplate) {
        templates[template.id] = template
    }

    fun getTemplate(id: String): TraitTemplate? = templates[id]
    fun getAllIds(): Set<String> = templates.keys

    fun updateTraits(unit: PlacedObject, deltaTime: Float, traitIds: List<String>, healthMap: Map<PlacedObject, Float>) {
        tools.healthMap = healthMap
        for (id in traitIds) {
            templates[id]?.onUpdate(unit, deltaTime, tools)
        }
    }

    fun tick(allUnits: List<PlacedObject>, deltaTime: Float) {
        tools.healthMap = allUnits.associateBy({ it }, { it.health.toFloat() })

        val currentAlive = mutableSetOf<Long>()
        val currentDataMap = mutableMapOf<Long, TraitUnitData>()

        for (unit in allUnits) {
            if (unit.traitIds.isEmpty()) continue
            val hp = unit.health
            if (hp <= 0) continue

            for (id in unit.traitIds) {
                templates[id]?.onUpdate(unit, deltaTime, tools)
            }

            currentAlive.add(unit.uniqueId)
            currentDataMap[unit.uniqueId] = TraitUnitData(
                traitIds = unit.traitIds.toList(),
                worldX = unit.worldX,
                worldY = unit.worldY,
                targetX = unit.targetX,
                targetY = unit.targetY,
                missileHealth = unit.missileHealth,
                suppressionRangeDisplayId = unit.suppressionRangeDisplayId,
                suppressionEffectId = unit.suppressionEffectId
            )
        }

        val deadUnitIds = lastFrameAliveTraitUnits - currentAlive

        for (deadId in deadUnitIds) {
            val deadData = unitTraitDataMap[deadId] ?: continue

            for (traitId in deadData.traitIds) {
                val template = templates[traitId] ?: continue
                if (template.isGlobal) {
                    globalInstances.add(GlobalTraitInstance(
                        traitId = traitId,
                        worldX = deadData.worldX,
                        worldY = deadData.worldY,
                        targetX = deadData.targetX,
                        targetY = deadData.targetY,
                        missileHealth = deadData.missileHealth,
                        displayId = deadData.suppressionRangeDisplayId,
                        suppressionEffectId = deadData.suppressionEffectId
                    ))
                }
            }
        }

        lastFrameAliveTraitUnits = currentAlive
        unitTraitDataMap.clear()
        unitTraitDataMap.putAll(currentDataMap)

        val globalIterator = globalInstances.iterator()
        while (globalIterator.hasNext()) {
            val instance = globalIterator.next()
            val template = templates[instance.traitId] ?: continue
            val virtualHost = PlacedObject().apply {
                this.worldX = instance.worldX
                this.worldY = instance.worldY
                this.targetX = instance.targetX
                this.targetY = instance.targetY
                this.missileHealth = instance.missileHealth
                this.suppressionRangeDisplayId = instance.displayId
                this.suppressionEffectId = instance.suppressionEffectId
            }
            val finished = template.onAfterDeath(virtualHost, deltaTime, tools)
            if (finished) {
                globalIterator.remove()
            }
        }
    }

    fun addGlobalInstance(traitId: String, worldX: Float, worldY: Float, targetX: Float, targetY: Float) {
        globalInstances.add(GlobalTraitInstance(
            traitId = traitId,
            worldX = worldX,
            worldY = worldY,
            targetX = targetX,
            targetY = targetY,
            missileHealth = 0
        ))
    }
}