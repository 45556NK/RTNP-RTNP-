// app/src/main/kotlin/com/rtnp/demo/logic/UnitUtils.kt
package com.rtnp.demo.logic

import android.graphics.*
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.movement.weapon.WeaponManager

object UnitUtils {

    // ========== 碰撞检测（带旋转） ==========

    @JvmStatic
    fun isPointInObject(px: Float, py: Float, o: PlacedObject): Boolean {
        val dx = px - o.worldX
        val dy = py - o.worldY

        // ★ 贴图单位额外旋转 90 度，碰撞检测需要同步
        val angleOffset = if (o.textureBitmap != null) 90f else 0f
        val angleRad = Math.toRadians((-o.heading + angleOffset).toDouble())
        val cos = Math.cos(angleRad).toFloat()
        val sin = Math.sin(angleRad).toFloat()
        val localX = dx * cos - dy * sin
        val localY = dx * sin + dy * cos

        return when (o.shape) {
            "circle" -> {
                val r = o.size / 2f
                localX * localX + localY * localY <= r * r
            }
            "square" -> {
                val h = o.size / 2f
                Math.abs(localX) <= h && Math.abs(localY) <= h
            }
            else -> {
                val hw = o.width / 2f
                val hh = o.height / 2f
                Math.abs(localX) <= hw && Math.abs(localY) <= hh
            }
        }
    }

    // ========== 布局计算 ==========

    @JvmStatic
    fun calculateVerticalLayout(startY: Int, spacing: Int, vararg heights: Int): IntArray {
        val tops = IntArray(heights.size)
        var currentY = startY
        for (i in heights.indices) {
            tops[i] = currentY
            currentY += heights[i] + spacing
        }
        return tops
    }

    // ========== 绘制工具 ==========

    @JvmStatic
    fun drawButton(c: Canvas, l: Int, t: Int, s: Int, txt: String, act: Boolean) {
        drawButton(c, l, t, s, txt, act, Color.YELLOW)
    }

    @JvmStatic
    fun drawButton(c: Canvas, l: Int, t: Int, s: Int, txt: String, act: Boolean, activeColor: Int) {
        val bg = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        c.drawRect(l.toFloat(), t.toFloat(), (l + s).toFloat(), (t + s).toFloat(), bg)

        val bd = Paint().apply {
            color = if (act) activeColor else Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        c.drawRect(l.toFloat(), t.toFloat(), (l + s).toFloat(), (t + s).toFloat(), bd)

        val tx = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        c.drawText(txt, (l + s / 2f), (t + s / 2f + 10f), tx)
    }

    @JvmStatic
    fun drawProgressCircle(c: Canvas, cx: Float, cy: Float, rad: Float, sw: Float, col: Int, prog: Float) {
        if (prog <= 0f) return
        val p = Paint().apply {
            isAntiAlias = true
            color = col
            style = Paint.Style.STROKE
            strokeWidth = sw
        }
        val oval = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        c.drawArc(oval, -90f, 360f * prog, false, p)
    }

    @JvmStatic
    fun drawWeaponButtonMulti(
        c: Canvas, l: Int, t: Int, s: Int,
        mainText: String, subText: String?,
        ws: PlacedObject.WeaponSlot, highlight: Boolean
    ) {
        val bg = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
        c.drawRect(l.toFloat(), t.toFloat(), (l + s).toFloat(), (t + s).toFloat(), bg)

        val bd = Paint().apply {
            color = if (highlight) Color.YELLOW else Color.WHITE
            style = Paint.Style.STROKE; strokeWidth = 3f
        }
        c.drawRect(l.toFloat(), t.toFloat(), (l + s).toFloat(), (t + s).toFloat(), bd)

        val tp = Paint().apply {
            color = Color.WHITE; textSize = 24f; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        c.drawText(mainText, (l + s / 2f), t + s * 0.45f, tp)

        if (!subText.isNullOrEmpty()) {
            val sp = Paint().apply {
                color = Color.LTGRAY; textSize = 20f; isAntiAlias = true; textAlign = Paint.Align.CENTER
            }
            c.drawText(subText, (l + s / 2f), t + s * 0.75f, sp)
        }

        if (!ws.active) return

        val cx = l + s / 2f; val cy = t + s / 2f; val rad = s / 2f - 4; val sw = s / 6f

        val weaponBar = ws.cooldownBar
        if (weaponBar != null && !weaponBar.isFinished && ws.cooldown > 0) {
            val arc = Paint().apply {
                isAntiAlias = true; color = Color.rgb(255, 102, 102); style = Paint.Style.STROKE; strokeWidth = sw
            }
            val oval = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
            c.drawArc(oval, -90f, 360f * weaponBar.progress, false, arc)
        }

        if (ws.type == "laser" && ws.chargeTime > 0) {
            val prog = (ws.laserCharge / ws.chargeTime).coerceIn(0f, 1f)
            if (prog > 0f) {
                val arc = Paint().apply {
                    isAntiAlias = true; color = Color.rgb(255, 102, 102); style = Paint.Style.STROKE; strokeWidth = sw
                }
                val oval = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
                c.drawArc(oval, -90f, 360f * prog, false, arc)
            }
        }

        if (ws.type == "pulse") {
            val pw = ws.pulseWeapon ?: return
            if (pw.energyBar.progress < 1f) {
                drawProgressCircle(c, cx, cy, rad - sw - 2, sw * 0.5f, Color.rgb(100, 200, 255), pw.energyBar.progress)
            }
            if (!pw.cooldownBar.isFinished) {
                drawProgressCircle(c, cx, cy, rad, sw, Color.rgb(255, 102, 102), pw.cooldownBar.progress)
            }
        }
    }
}