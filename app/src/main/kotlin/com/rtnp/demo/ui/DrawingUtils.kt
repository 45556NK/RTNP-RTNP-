package com.rtnp.demo.ui

import android.graphics.*
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.render.button.ProgressBarTool

object DrawingUtils {

   @JvmStatic
   fun drawButton(c: Canvas, l: Int, t: Int, s: Int, txt: String, act: Boolean, activeColor: Int) {
       val bg = Paint().apply {
           color = Color.BLACK; style = Paint.Style.FILL
       }
       c.drawRect(l.toFloat(), t.toFloat(), (l + s).toFloat(), (t + s).toFloat(), bg)

       val bd = Paint().apply {
           color = if (act) activeColor else Color.WHITE
           style = Paint.Style.STROKE; strokeWidth = 3f
       }
       c.drawRect(l.toFloat(), t.toFloat(), (l + s).toFloat(), (t + s).toFloat(), bd)

       val tx = Paint().apply {
           color = Color.WHITE; textSize = 28f; isAntiAlias = true; textAlign = Paint.Align.CENTER
       }
       
       // ★ 支持换行
       if (txt.contains("\n")) {
           val lines = txt.split("\n")
           val lineHeight = tx.textSize + 4f
           val totalHeight = lines.size * lineHeight
           val startY = t + s / 2f - totalHeight / 2f + tx.textSize
           for (i in lines.indices) {
               c.drawText(lines[i], (l + s / 2f), startY + i * lineHeight, tx)
           }
       } else {
           c.drawText(txt, (l + s / 2f), (t + s / 2f + 10f), tx)
       }
   }

    @JvmStatic
    fun drawButton(c: Canvas, l: Int, t: Int, s: Int, txt: String, act: Boolean) {
        drawButton(c, l, t, s, txt, act, Color.YELLOW)
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
        c: Canvas,
        l: Int,
        t: Int,
        s: Int,
        mainText: String,
        subText: String?,
        ws: PlacedObject.WeaponSlot,
        highlight: Boolean
    ) {
        val bg = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        c.drawRect(l.toFloat(), t.toFloat(), (l + s).toFloat(), (t + s).toFloat(), bg)

        val bd = Paint().apply {
            color = if (highlight) Color.YELLOW else Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        c.drawRect(l.toFloat(), t.toFloat(), (l + s).toFloat(), (t + s).toFloat(), bd)

        val tp = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val mainY = t + s * 0.45f
        c.drawText(mainText, (l + s / 2f), mainY, tp)

        if (!subText.isNullOrEmpty()) {
            val sp = Paint().apply {
                color = Color.LTGRAY
                textSize = 20f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val subY = t + s * 0.75f
            c.drawText(subText, (l + s / 2f), subY, sp)
        }

        if (!ws.active) return

        // 通用变量
        val cx = l + s / 2f
        val cy = t + s / 2f
        val rad = s / 2f - 4
        val sw = s / 6f

        // 武器冷却圈（红色）
        val weaponBar = ws.cooldownBar
        if (weaponBar != null && !weaponBar.isFinished && ws.cooldown > 0) {
            val arc = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(255, 102, 102)
                style = Paint.Style.STROKE
                strokeWidth = sw
            }
            val oval = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
            c.drawArc(oval, -90f, 360f * weaponBar.progress, false, arc)
        }

        // 激光充能圈
        if (ws.type == "laser" && ws.chargeTime > 0) {
            val prog = (ws.laserCharge / ws.chargeTime).coerceIn(0f, 1f)
            if (prog > 0f) {
                val arc = Paint().apply {
                    isAntiAlias = true
                    color = Color.rgb(255, 102, 102)
                    style = Paint.Style.STROKE
                    strokeWidth = sw
                }
                val oval = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
                c.drawArc(oval, -90f, 360f * prog, false, arc)
            }
        }

        // 脉冲武器：能量条（浅蓝色）+ 冷却圈
        if (ws.type == "pulse") {
            val pw = ws.pulseWeapon ?: return
            // 能量条（浅蓝色内圈）
            if (pw.energyBar.progress < 1f) {
                drawProgressCircle(c, cx, cy, rad - sw - 2, sw * 0.5f, Color.rgb(100, 200, 255), pw.energyBar.progress)
            }
            // 冷却圈（红色外圈）
            if (!pw.cooldownBar.isFinished) {
                drawProgressCircle(c, cx, cy, rad, sw, Color.rgb(255, 102, 102), pw.cooldownBar.progress)
            }
        }
    }

    @JvmStatic
    fun drawStrategyButton(
        c: Canvas,
        l: Int,
        t: Int,
        s: Int,
        text: String,
        isActive: Boolean,
        cooldownBar: ProgressBarTool?
    ) {
        val bg = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        c.drawRect(l.toFloat(), t.toFloat(), (l + s).toFloat(), (t + s).toFloat(), bg)

        val bd = Paint().apply {
            color = if (isActive) Color.rgb(160, 32, 240) else Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        c.drawRect(l.toFloat(), t.toFloat(), (l + s).toFloat(), (t + s).toFloat(), bd)

        val tp = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        c.drawText(text, (l + s / 2f), (t + s / 2f + 10f), tp)

        if (cooldownBar != null && !cooldownBar.isFinished) {
            val cx = l + s / 2f
            val cy = t + s / 2f
            val rad = s / 2f - 4
            val sw = s / 6f
            val arc = Paint().apply {
                isAntiAlias = true
                color = Color.rgb(160, 32, 240)
                style = Paint.Style.STROKE
                strokeWidth = sw
            }
            val oval = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
            c.drawArc(oval, -90f, 360f * cooldownBar.progress, false, arc)
        }
    }
}