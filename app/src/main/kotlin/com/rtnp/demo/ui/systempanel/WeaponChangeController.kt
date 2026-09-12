package com.rtnp.demo.ui.systempanel

import android.graphics.Color
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.MissileTypeRegistry
import com.rtnp.demo.movement.ModuleManager
import com.rtnp.demo.movement.weapon.WeaponManager
import com.rtnp.demo.ui.systempanel.tools.SystemPanelChangeTool
import com.rtnp.demo.TranslationTable

object WeaponChangeController {

    fun show(
        unitSystem: UnitSystem?,
        slotIndex: Int,
        startX: Float,
        startY: Float,
        onFinished: () -> Unit
    ) {
        val unit = unitSystem?.selectedUnit
        if (unit == null) {
            onFinished()
            return
        }

        val hexItems = mutableListOf<SystemPanelChangeTool.ChangeHexItem>()
        hexItems.add(
            SystemPanelChangeTool.ChangeHexItem(
                backgroundColor = Color.rgb(255, 165, 0),
                label = "返回"
            )
        )

        val weaponIds = WeaponManager.getAllTemplates().keys.toList()
        for (weaponId in weaponIds) {
            val template = WeaponManager.getTemplate(weaponId) ?: continue
            val displayName = if (template.subType.isNotEmpty()) {
                "${template.displayName} ${template.subType}"
            } else {
                template.displayName
            }
            // ★ 有贴图则只传贴图路径，label 为空；无贴图才传文字
            if (template.iconPath != null) {
                hexItems.add(
                    SystemPanelChangeTool.ChangeHexItem(
                        backgroundColor = Color.rgb(255, 130, 130),
                        label = "",  // 不传文字
                        iconPath = template.iconPath
                    )
                )
            } else {
                hexItems.add(
                    SystemPanelChangeTool.ChangeHexItem(
                        backgroundColor = Color.rgb(255, 130, 130),
                        label = displayName,
                        iconPath = null
                    )
                )
            }
        }

        SystemPanelChangeTool.show(
            startX = startX,
            startY = startY,
            topText = "",
            hexItems = hexItems,
            listener = object : SystemPanelChangeTool.ChangeToolListener {
                private var selectedWeaponIndex = -1

                override fun onHexSelected(index: Int, centerX: Float, centerY: Float) {
                    if (index == 0) {
                        SystemPanelChangeTool.hide()
                        onFinished()
                    } else {
                        selectedWeaponIndex = index - 1
                        val weaponId = weaponIds.getOrNull(selectedWeaponIndex)
                        val template = weaponId?.let { WeaponManager.getTemplate(it) }
                        if (template != null) {
                            val infoText = buildWeaponInfoText(template)
                            SystemPanelChangeTool.setTopText(infoText)
                        }
                        SystemPanelChangeTool.showBottomConfirm()
                    }
                }

                override fun onBottomButtonClicked(buttonType: SystemPanelChangeTool.BottomButtonType) {
                    when (buttonType) {
                        SystemPanelChangeTool.BottomButtonType.CANCEL -> {
                            SystemPanelChangeTool.hideBottomConfirm()
                            SystemPanelChangeTool.resetSelection()
                        }
                        SystemPanelChangeTool.BottomButtonType.CONFIRM -> {
                            if (selectedWeaponIndex >= 0 && selectedWeaponIndex < weaponIds.size) {
                                val weaponId = weaponIds[selectedWeaponIndex]
                                val slot = ModuleManager.createSlotFromTemplate(weaponId)
                                if (slot != null && slotIndex in unit.weaponSlots.indices) {
                                    unit.weaponSlots[slotIndex] = slot
                                    unit.syncCurrentWeapon()
                                }
                            }
                            SystemPanelChangeTool.hide()
                            onFinished()
                        }
                    }
                }
            }
        )
    }

    private fun buildWeaponInfoText(template: com.rtnp.demo.movement.weapon.WeaponTemplate): String {
        val sb = StringBuilder()
        when (template.category) {
            "railgun" -> {
                sb.appendLine("${TranslationTable.get("attr_damage")}: ${template.damage}")
                sb.appendLine("${TranslationTable.get("attr_cooldown")}: ${template.cooldown}")
                val fireRate = if (template.cooldown > 0) 1f / template.cooldown else 0f
                sb.appendLine("${TranslationTable.get("attr_fire_rate")}: ${"%.1f".format(fireRate)}")
                sb.append("${TranslationTable.get("attr_range")}: ${template.range}")
            }
            "laser" -> {
                sb.appendLine("${TranslationTable.get("attr_damage")}: ${template.minDamage}-${template.maxDamage}")
                sb.append("${TranslationTable.get("attr_charge_time")}: ${template.chargeTime}")
            }
            "missile" -> {
                sb.appendLine("${TranslationTable.get("attr_range")}: ${template.range}")
                sb.appendLine("${TranslationTable.get("attr_cooldown")}: ${template.cooldown}")
                sb.appendLine("${TranslationTable.get("attr_min_range")}: ${template.minRange}")
                val missileType = template.missileTypeName?.let { MissileTypeRegistry.get(it) }
                val explosionDamage = missileType?.damage ?: 0
                val explosionRange = missileType?.explosionRange ?: 0
                sb.appendLine("${TranslationTable.get("attr_explosion_damage")}: $explosionDamage")
                sb.append("${TranslationTable.get("attr_explosion_range")}: $explosionRange")
            }
            "pulse" -> {
                sb.appendLine("${TranslationTable.get("attr_damage")}: ${template.damage}")
                sb.appendLine("${TranslationTable.get("attr_range")}: ${template.range}")
                sb.appendLine("${TranslationTable.get("attr_burst_time")}: ${template.energyDuration}")
                sb.appendLine("${TranslationTable.get("attr_cooldown")}: ${template.cooldown}")
                sb.appendLine("${TranslationTable.get("attr_energy_regen_time")}: ${template.energyRegenDelay}")
                sb.append("${TranslationTable.get("attr_max_targets")}: ${template.maxTargets}")
            }
            else -> {
                sb.append("未知武器")
            }
        }
        return sb.toString()
    }
}