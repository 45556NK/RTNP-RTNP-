package com.rtnp.demo.testing

import android.graphics.*
import com.rtnp.demo.combat.MissileLoop
import com.rtnp.demo.core.PlacedObject

object UnitBoundsRenderer {

    private val boundsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 255, 100, 100)
        style = Paint.Style.STROKE
    }

    private val rectPath = Path()

    fun draw(canvas: Canvas, zoom: Float, units: List<PlacedObject>) {
        boundsPaint.strokeWidth = 3f / zoom

        for (unit in units) {
            val extra = if (unit.category == "missile") MissileLoop.MISSILE_HITBOX_EXTRA else 0f

            canvas.save()
            canvas.translate(unit.worldX, unit.worldY)
            
            // ★ 有贴图的单位额外旋转90度，与贴图绘制保持一致
            if (unit.textureBitmap != null) {
                canvas.rotate(unit.heading + 90f)
            } else {
                canvas.rotate(unit.heading)
            }

            when (unit.shape) {
                "circle" -> {
                    val radius = unit.size / 2f + extra
                    canvas.drawCircle(0f, 0f, radius, boundsPaint)
                }
                "square" -> {
                    val half = unit.size / 2f + extra
                    rectPath.reset()
                    rectPath.addRect(-half, -half, half, half, Path.Direction.CW)
                    canvas.drawPath(rectPath, boundsPaint)
                }
                else -> {
                    val hw = unit.width / 2f + extra
                    val hh = unit.height / 2f + extra
                    rectPath.reset()
                    rectPath.addRect(-hw, -hh, hw, hh, Path.Direction.CW)
                    canvas.drawPath(rectPath, boundsPaint)
                }
            }

            canvas.restore()
        }
    }
}