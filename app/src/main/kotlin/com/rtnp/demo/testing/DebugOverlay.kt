// app/src/main/kotlin/com/rtnp/demo/testing/DebugOverlay.kt
package com.rtnp.demo.testing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.trait.TraitManager
import com.rtnp.demo.combat.WeaponConstants
import com.rtnp.demo.ui.ToastTool
import com.rtnp.demo.render.RenderControl

object DebugOverlay {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
    }

    private var lastFpsTime = 0L
    private var frameCount = 0
    private var currentFps = 0f

    fun draw(
        canvas: Canvas,
        zoom: Float,
        objectCount: Int,
        unitSystem: UnitSystem
    ) {
        val now = System.currentTimeMillis()
        if (lastFpsTime == 0L) lastFpsTime = now
        frameCount++
        if (now - lastFpsTime >= 1000) {
            currentFps = frameCount * 1000f / (now - lastFpsTime)
            frameCount = 0
            lastFpsTime = now
        }

        var y = 50

        // FPS
        paint.color = when {
            currentFps >= 55 -> Color.GREEN
            currentFps >= 30 -> Color.YELLOW
            else -> Color.RED
        }
        paint.textSize = 40f
        canvas.drawText("FPS: ${"%.1f".format(currentFps)}", 20f, y.toFloat(), paint)
        y += 45

        // 缩放与单位数
        paint.color = Color.WHITE
        paint.textSize = 28f
        canvas.drawText("缩放: ${"%.2f".format(zoom)}x  单位: $objectCount", 20f, y.toFloat(), paint)
        y += 35

        // 全局特性
        paint.color = Color.CYAN
        paint.textSize = 24f
        val allTraitIds = TraitManager.getAllIds()
        canvas.drawText("全局特性: ${allTraitIds.joinToString(", ")}", 20f, y.toFloat(), paint)
        y += 30

        // 选中单位信息
        val sel = unitSystem.selectedUnit
        if (sel != null) {
            paint.color = Color.YELLOW
            canvas.drawText("选中: ${sel.displayName ?: sel.name}", 20f, y.toFloat(), paint)
            y += 30

            // 停泊调试信息
            paint.color = Color.rgb(255, 200, 100)
            paint.textSize = 22f
            canvas.drawText("停泊: target=${sel.dockTarget?.name ?: "null"} action=${sel.actionDock} docked=${sel.isDocked}", 20f, y.toFloat(), paint)
            y += 26
            canvas.drawText("  moving=${sel.isMoving} stop=${sel.isStopping} speed=${"%.0f".format(sel.currentSpeed)}", 20f, y.toFloat(), paint)
            y += 26
            canvas.drawText("  pos=(${"%.0f".format(sel.worldX)},${"%.0f".format(sel.worldY)}) tgt=(${"%.0f".format(sel.targetX)},${"%.0f".format(sel.targetY)})", 20f, y.toFloat(), paint)
            y += 26
            canvas.drawText("  preview=(${"%.0f".format(sel.previewTargetX)},${"%.0f".format(sel.previewTargetY)}) hasPreview=${sel.hasPreviewTarget}", 20f, y.toFloat(), paint)
            y += 26
            canvas.drawText("  rotating=${sel.isRotating} dockSlot=${sel.dockSlotIndex}", 20f, y.toFloat(), paint)
            y += 30

            // 运动信息
            paint.color = Color.WHITE
            canvas.drawText(
                "角度: ${"%.1f".format(sel.heading)}°  速度: ${"%.1f".format(sel.currentSpeed)}",
                20f, y.toFloat(), paint
            )
            y += 28

            // 单位拥有的特性
            val unitTraits = sel.traitIds?.joinToString(", ") ?: "无"
            paint.color = Color.CYAN
            canvas.drawText("单位特性: $unitTraits", 20f, y.toFloat(), paint)
            y += 28

            // 策略槽信息
            if (sel.strategySlots > 0) {
                for (i in 0 until sel.strategySlots) {
                    val sid = sel.strategyIds.getOrNull(i) ?: "空"
                    paint.color = Color.MAGENTA
                    canvas.drawText("策略$i: $sid", 20f, y.toFloat(), paint)
                    y += 28
                }
            }

            // 武器槽信息
            if (sel.weaponSlots.isNotEmpty()) {
                for (i in sel.weaponSlots.indices) {
                    val ws = sel.weaponSlots[i]
                    if (!ws.active) continue

                    val target = ws.lockedTarget
                    val targetInfo = if (target != null) {
                        "${target.name}(速:${"%.0f".format(target.currentSpeed)})"
                    } else "无"

                    paint.color = Color.WHITE
                    canvas.drawText(
                        "武器$i: ${ws.type} → $targetInfo  偏移:${"%.2f".format(Math.toDegrees(ws.aimAngleOffset.toDouble()))}°",
                        20f, y.toFloat(), paint
                    )
                    y += 28

                    val bar = ws.cooldownBar
                    if (bar != null && !bar.isFinished && ws.cooldown > 0) {
                        paint.color = Color.rgb(255, 200, 100)
                        canvas.drawText("  冷却: ${"%.0f".format(bar.progress * 100)}%", 20f, y.toFloat(), paint)
                        y += 28
                    }
                    if (ws.type == "pulse") {
                        val pw = ws.pulseWeapon
                        if (pw != null) {
                            paint.color = Color.CYAN
                            canvas.drawText("  能量: ${"%.0f".format(pw.energyBar.progress * 100)}% 冷却: ${"%.0f".format(pw.cooldownBar.progress * 100)}%", 20f, y.toFloat(), paint)
                            y += 28
                            for (t in pw.lockedTargets) {
                                val dx = t.worldX - sel.worldX
                                val dy = t.worldY - sel.worldY
                                val dist2 = dx * dx + dy * dy
                                val range2 = ws.range * ws.range
                                canvas.drawText("  目标:${t.name} 距离²:$dist2 射程²:$range2 超范围:${dist2 > range2}", 20f, y.toFloat(), paint)
                                y += 28
                            }
                        }
                    }
                }
            }
        }

        // 贴图单位
        var textureCount = 0
        for (obj in unitSystem.placedObjects) {
            if (obj.textureBitmap != null && obj.category != "missile") {
                textureCount++
            }
        }
        paint.color = Color.WHITE
        paint.textSize = 24f
        canvas.drawText("贴图单位: $textureCount", 20f, y.toFloat(), paint)
        y += 30

        // 倍速
        canvas.drawText("倍速: ${"%.1f".format(WeaponConstants.bulletSpeedMultiplier)}x", 20f, y.toFloat(), paint)
        y += 30

        // Toast 状态
        paint.color = Color.rgb(255, 200, 100)
        paint.textSize = 22f
        val inst = ToastTool.globalInstance
        canvas.drawText("Toast: inst=${inst != null} vis=${inst?.isVisible ?: false} msg=${inst?.let { if (it.isVisible) "有" else "无" } ?: "null"}", 20f, y.toFloat(), paint)
        y += 28

        // FIXED_DT
        paint.color = if (UnitSystem.FIXED_DT <= 0f || UnitSystem.FIXED_DT > 0.1f) Color.RED else Color.WHITE
        canvas.drawText("FIXED_DT: ${"%.4f".format(UnitSystem.FIXED_DT)}s", 20f, y.toFloat(), paint)
        y += 28

        // 帧时间
        paint.color = Color.WHITE
        val frameMs = if (currentFps > 0) (1000f / currentFps) else 0f
        canvas.drawText("帧时间: ${"%.1f".format(frameMs)}ms", 20f, y.toFloat(), paint)
        y += 28

        // 对象总数
        val missileCount = unitSystem.placedObjects.count { it.category == "missile" }
        paint.color = Color.LTGRAY
        canvas.drawText("对象: ${unitSystem.placedObjects.size} (导弹$missileCount) | 子弹: ${unitSystem.bullets.size}", 20f, y.toFloat(), paint)
        y += 28

        // UI 禁用状态
        paint.color = Color.rgb(255, 100, 100)
        paint.textSize = 22f
        canvas.drawText("UI禁用: ${RenderControl.touchDisabledUnitInfoPanel} | 底部栏: ${RenderControl.touchDisabledBottomBar}", 20f, y.toFloat(), paint)
        y += 26
    }
}