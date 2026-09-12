// app/src/main/kotlin/com/rtnp/demo/gpu/render/tools/MovePathTool.kt
package com.rtnp.demo.gpu.render.tools

import com.rtnp.demo.core.PlacedObject
import kotlin.math.sqrt

class MovePathTool {

    private var currentUnits: List<PlacedObject> = emptyList()
    private val moveVertices = mutableListOf<Float>()
    private val previewDots = mutableListOf<Float>()

    fun buildMovePaths(units: List<PlacedObject>) {
        currentUnits = units.toList()
        moveVertices.clear()
        previewDots.clear()
        for (u in currentUnits) {
            if (u.type == "base" || u.category == "missile") continue

            if (u.isMoving) {
                addDashedLine(moveVertices, u.worldX, u.worldY, u.targetX, u.targetY, 20f, 5f)
            }

            if (u.hasPreviewTarget && !u.isMoving) {
                addDots(previewDots, u.worldX, u.worldY, u.previewTargetX, u.previewTargetY)
            }
        }
    }

    /**
     * ★ 强制清除指定单位的预览路线
     */
    fun clearPreviewFor(unit: PlacedObject) {
        currentUnits = currentUnits.filter { it !== unit }
        moveVertices.clear()
        previewDots.clear()
        for (u in currentUnits) {
            if (u.type == "base" || u.category == "missile") continue

            if (u.isMoving) {
                addDashedLine(moveVertices, u.worldX, u.worldY, u.targetX, u.targetY, 20f, 5f)
            }

            if (u.hasPreviewTarget && !u.isMoving) {
                addDots(previewDots, u.worldX, u.worldY, u.previewTargetX, u.previewTargetY)
            }
        }
    }

    private fun addDashedLine(list: MutableList<Float>, x1: Float, y1: Float, x2: Float, y2: Float, segLen: Float, gapLen: Float) {
        val dx = x2 - x1; val dy = y2 - y1
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= 0f) return
        val normX = dx / dist; val normY = dy / dist
        val cycle = segLen + gapLen
        var pos = 0f
        var drawing = true
        while (pos < dist) {
            val start = pos.coerceAtLeast(0f)
            val end = if (drawing) (pos + segLen).coerceAtMost(dist) else (pos + gapLen).coerceAtMost(dist)
            if (drawing && end > start) {
                list.add(x1 + normX * start)
                list.add(y1 + normY * start)
                list.add(x1 + normX * end)
                list.add(y1 + normY * end)
            }
            pos += if (drawing) segLen else gapLen
            drawing = !drawing
        }
    }

    private fun addDots(list: MutableList<Float>, x1: Float, y1: Float, x2: Float, y2: Float) {
        val dx = x2 - x1; val dy = y2 - y1
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= 0f) return
        val normX = dx / dist; val normY = dy / dist
        val dotDiameter = 5f
        val gap = 3f
        val step = dotDiameter + gap
        var pos = dotDiameter / 2f
        while (pos < dist) {
            list.add(x1 + normX * pos)
            list.add(y1 + normY * pos)
            pos += step
        }
    }

    fun getMoveVertices2D(): FloatArray = moveVertices.toFloatArray()
    fun getPreviewDots(): FloatArray = previewDots.toFloatArray()
    fun isMoveEmpty(): Boolean = moveVertices.isEmpty()
    fun isPreviewEmpty(): Boolean = previewDots.isEmpty()
}