// app/src/main/kotlin/com/rtnp/demo/core/BuildListRegistry.kt
package com.rtnp.demo.core

object BuildListRegistry {

    // ★ 建造列表：只存模板 ID
    private val buildIds = mutableListOf<String>()

    fun addBuildId(id: String) {
        if (!buildIds.contains(id)) buildIds.add(id)
    }

    fun removeBuildId(id: String) {
        buildIds.remove(id)
    }

    fun getBuildIds(): List<String> = buildIds.toList()

    /** 根据 ID 从 TemplateRegistry 获取完整模板 */
    fun getBuildTemplates(): List<InventoryItem> {
        return buildIds.mapNotNull { id ->
            TemplateRegistry.templates.firstOrNull { it.name == id }
        }
    }

    fun clear() {
        buildIds.clear()
    }
}