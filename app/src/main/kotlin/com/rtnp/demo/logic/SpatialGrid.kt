package com.rtnp.demo.logic

import com.rtnp.demo.core.PlacedObject

class SpatialGrid(
    private val worldWidth: Float,
    private val worldHeight: Float,
    private val cellSize: Float = 200f
) {
    private val cols = (worldWidth / cellSize).toInt() + 1
    private val rows = (worldHeight / cellSize).toInt() + 1
    private val cells = Array(cols * rows) { mutableListOf<PlacedObject>() }

    /** 填充所有单位，不清空列表而是逐格重新 assign */
    fun rebuild(allObjects: List<PlacedObject>) {
        for (cell in cells) cell.clear()
        for (obj in allObjects) {
            val idx = indexOf(obj.worldX, obj.worldY) ?: continue
            cells[idx].add(obj)
        }
    }

    /**
     * 查询线段 (x1,y1)→(x2,y2) 沿线可能相交的单位（含相邻格子扩展）。
     * 结果复用传入的 outList，避免分配。
     */
    fun queryAlongSegment(x1: Float, y1: Float, x2: Float, y2: Float, outList: MutableList<PlacedObject>) {
        outList.clear()
        val seen = HashSet<Int>()
        // 起点、终点、中点格子
        addCellUnits(indexOf(x1, y1), seen, outList)
        addCellUnits(indexOf(x2, y2), seen, outList)
        if (distanceSq(x1, y1, x2, y2) > cellSize * cellSize) {
            addCellUnits(indexOf((x1 + x2) / 2f, (y1 + y2) / 2f), seen, outList)
        }
        // 扩展相邻格子
        val expanded = mutableListOf<PlacedObject>()
        for (obj in outList) {
            addNeighborUnits(obj, seen, expanded)
        }
        outList.addAll(expanded)
    }

    /** 查询圆形范围内的单位（用于爆炸伤害） */
    fun queryCircle(cx: Float, cy: Float, radius: Float, outList: MutableList<PlacedObject>) {
        outList.clear()
        val seen = HashSet<Int>()
        val r2 = radius * radius
        val minCol = ((cx - radius) / cellSize).toInt().coerceIn(0, cols - 1)
        val maxCol = ((cx + radius) / cellSize).toInt().coerceIn(0, cols - 1)
        val minRow = ((cy - radius) / cellSize).toInt().coerceIn(0, rows - 1)
        val maxRow = ((cy + radius) / cellSize).toInt().coerceIn(0, rows - 1)

        for (row in minRow..maxRow) {
            for (col in minCol..maxCol) {
                val idx = row * cols + col
                for (obj in cells[idx]) {
                    val dx = obj.worldX - cx
                    val dy = obj.worldY - cy
                    if (dx * dx + dy * dy <= r2) {
                        if (seen.add(obj.hashCode())) {
                            outList.add(obj)
                        }
                    }
                }
            }
        }
    }

    private fun addCellUnits(idx: Int?, seen: HashSet<Int>, out: MutableList<PlacedObject>) {
        if (idx == null) return
        for (obj in cells[idx]) {
            if (seen.add(obj.hashCode())) out.add(obj)
        }
    }

    private fun addNeighborUnits(obj: PlacedObject, seen: HashSet<Int>, out: MutableList<PlacedObject>) {
        val idx = indexOf(obj.worldX, obj.worldY) ?: return
        val row = idx / cols
        val col = idx % cols
        for (dr in -1..1) {
            for (dc in -1..1) {
                val nr = row + dr
                val nc = col + dc
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                for (neighbor in cells[nr * cols + nc]) {
                    if (seen.add(neighbor.hashCode())) out.add(neighbor)
                }
            }
        }
    }

    private fun indexOf(x: Float, y: Float): Int? {
        val col = (x / cellSize).toInt()
        val row = (y / cellSize).toInt()
        if (col < 0 || col >= cols || row < 0 || row >= rows) return null
        return row * cols + col
    }

    private fun distanceSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return dx * dx + dy * dy
    }
}