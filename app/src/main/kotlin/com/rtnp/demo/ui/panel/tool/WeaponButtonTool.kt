// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/WeaponButtonTool.kt
package com.rtnp.demo.ui.panel.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.movement.weapon.WeaponManager
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent

object WeaponButtonTool {

    private const val BORDER_IMAGE = "images/weapon_border.svg"
    private const val OFF_OVERLAY = "images/ui/weapon_off_overlay.png"

    private val cooldownConfigs = mutableMapOf<PlacedObject.WeaponSlot, CooldownConfig>()

    class CooldownConfig(
        var color: Int = Color.argb(89, 255, 130, 130),
        var direction: CooldownRenderTool.Direction = CooldownRenderTool.Direction.BOTTOM_UP
    )

    fun create(slot: PlacedObject.WeaponSlot, index: Int, vw: Int, context: Context): InteractiveButtonComponent {
        val size = InteractiveButtonTool.calculateSize(vw)
        val borderBitmap = ImageManager.getInstance(context).getBitmap(BORDER_IMAGE)
        val iconBitmap = loadWeaponIcon(slot, context)
        val offOverlay = ImageManager.getInstance(context).getBitmap(OFF_OVERLAY)

        val label = if (iconBitmap != null) "" else buildLabel(slot, index)

        val templateId = slot.templateId
        val template = if (templateId != null) WeaponManager.getTemplate(templateId) else null
        val animLabel = if (template != null) {
            val displayName = template.displayName
            val subType = template.subType
            if (subType.isNotEmpty()) "$displayName $subType" else displayName
        } else ""

        val btn = InteractiveButtonComponent(
            label = label,
            width = size,
            height = size,
            backgroundColor = Color.argb(25, 255, 130, 130),
            borderBitmap = borderBitmap,
            iconBitmap = iconBitmap,
            animLabel = animLabel,
            onClick = {
                slot.active = !slot.active
            },
            onPressed = { i -> ButtonAnimTool.defaultOnPress(i) },
            onReleased = { i -> ButtonAnimTool.defaultOnRelease(i) },
            onCancelled = { i -> ButtonAnimTool.defaultOnCancel(i) }
        )
        btn.customData = WeaponButtonExtra(slot, offOverlay)
        return btn
    }

    private fun loadWeaponIcon(slot: PlacedObject.WeaponSlot, context: Context): Bitmap? {
        val templateId = slot.templateId ?: return null
        val autoPath = "images/icons/$templateId.svg"
        val bitmap = ImageManager.getInstance(context).getBitmap(autoPath)
        if (bitmap != null) return bitmap
        val template = WeaponManager.getTemplate(templateId) ?: return null
        val iconPath = template.iconPath ?: return null
        return ImageManager.getInstance(context).getBitmap(iconPath)
    }

    private fun buildLabel(slot: PlacedObject.WeaponSlot, index: Int): String {
        return "${slot.type} ${index + 1}"
    }

    fun isWeaponOff(comp: InteractiveButtonComponent): Boolean {
        val extra = comp.customData as? WeaponButtonExtra ?: return false
        return !extra.slot.active
    }

    fun getExtra(comp: InteractiveButtonComponent): WeaponButtonExtra? {
        return comp.customData as? WeaponButtonExtra
    }

    fun getAlpha(comp: InteractiveButtonComponent): Pair<Int, Int> {
        val isActive = !isWeaponOff(comp)
        return LayerAlphaTool.getAlphaForState(isActive)
    }

    fun setCooldownConfig(slot: PlacedObject.WeaponSlot, color: Int, direction: CooldownRenderTool.Direction) {
        val config = cooldownConfigs.getOrPut(slot) { CooldownConfig() }
        config.color = color
        config.direction = direction
    }

    fun updateCooldowns(buttons: List<InteractiveButtonComponent>) {
        for (btn in buttons) {
            val extra = btn.customData as? WeaponButtonExtra ?: continue
            val slot = extra.slot

            val bar = slot.cooldownBar ?: slot.pulseWeapon?.cooldownBar
            if (bar != null && !bar.isFinished) {
                btn.cooldownProgress = bar.progress
                val config = cooldownConfigs[slot]
                if (config != null) {
                    btn.cooldownColor = config.color
                    btn.cooldownDirection = config.direction
                }
            } else {
                btn.cooldownProgress = 0f
            }

            val laserProgress = if (slot.chargeTime > 0f) (slot.laserCharge / slot.chargeTime).coerceIn(0f, 1f) else null
            val pulseProgress = slot.pulseWeapon?.energyBar?.progress
            val energyProgress = laserProgress ?: pulseProgress
            EnergyRingTool.applyToButton(slot, btn, energyProgress)
        }
    }
}

data class WeaponButtonExtra(
    val slot: PlacedObject.WeaponSlot,
    val offOverlay: Bitmap?
)