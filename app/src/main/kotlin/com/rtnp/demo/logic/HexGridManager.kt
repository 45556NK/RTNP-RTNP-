package com.rtnp.demo.logic

import com.rtnp.demo.core.PlacedObject
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object HexGridManager {

    data class HexTile(
        val id: Int,
        val q: Int,
        val r: Int,
        val worldX: Float,
        val worldY: Float
    )

    @JvmField var hexSide: Float = 1000f
    val hexHoriz: Float get() = (sqrt(3.0) * hexSide).toFloat()
    val hexVert: Float get() = hexSide * 1.5f

    @JvmField var maxLayer: Int = 2
    @JvmField var worldCenterX: Float = 0f
    @JvmField var worldCenterY: Float = 0f

    private val tiles = mutableListOf<HexTile>()
    private val tileMap = mutableMapOf<Pair<Int, Int>, HexTile>()
    
    /** 区块ID -> 区块内单位列表 */
    private val tileUnits = mutableMapOf<Int, MutableList<PlacedObject>>()
    
    /** 单位 -> 所在区块ID */
    private val unitTile = mutableMapOf<PlacedObject, Int>()

    private var initialized = false

    fun init(centerX: Float, centerY: Float, layer: Int, side: Float) {
        worldCenterX = centerX
        worldCenterY = centerY
        maxLayer = layer
        hexSide = side

        tiles.clear()
        tileMap.clear()
        tileUnits.clear()
        unitTile.clear()

        var id = 0
        for (q in -maxLayer..maxLayer) {
            val r1 = max(-maxLayer, -q - maxLayer)
            val r2 = min(maxLayer, -q + maxLayer)
            for (r in r1..r2) {
                val worldX = centerX + hexHoriz * (q + r / 2f)
                val worldY = centerY + hexVert * r
                val tile = HexTile(id++, q, r, worldX, worldY)
                tiles.add(tile)
                tileMap[Pair(q, r)] = tile
                tileUnits[tile.id] = mutableListOf()
            }
        }
        initialized = true
    }

    // ==================== 单位管理 ====================

    /**
     * 将单位注册到所在区块
     */
    fun registerUnit(unit: PlacedObject) {
        if (!initialized) return
        val tile = getTileAt(unit.worldX, unit.worldY) ?: return
        tileUnits.getOrPut(tile.id) { mutableListOf() }.add(unit)
        unitTile[unit] = tile.id
    }

    /**
     * 移除单位
     */
    fun unregisterUnit(unit: PlacedObject) {
        val tileId = unitTile.remove(unit) ?: return
        tileUnits[tileId]?.remove(unit)
    }

    /**
     * 更新单位所在区块（单位移动后调用）
     */
    fun updateUnit(unit: PlacedObject) {
        val oldTileId = unitTile[unit]
        val newTile = getTileAt(unit.worldX, unit.worldY) ?: return
        if (oldTileId == newTile.id) return

        // 从旧区块移除
        if (oldTileId != null) {
            tileUnits[oldTileId]?.remove(unit)
        }
        // 加入新区块
        tileUnits.getOrPut(newTile.id) { mutableListOf() }.add(unit)
        unitTile[unit] = newTile.id
    }

    /**
     * 刷新所有单位的位置（批量重建）
     */
    fun refreshAllUnits(units: List<PlacedObject>) {
        // 清空
        for (list in tileUnits.values) list.clear()
        unitTile.clear()
        // 重新分配
        for (unit in units) {
            registerUnit(unit)
        }
    }

    // ==================== 查询 ====================

    /**
     * 获取指定区块ID中的所有单位
     */
    fun getUnitsInTile(tileId: Int): List<PlacedObject> {
        return tileUnits[tileId]?.toList() ?: emptyList()
    }

    /**
     * 获取指定坐标所在区块中的所有单位
     */
    fun getUnitsAt(worldX: Float, worldY: Float): List<PlacedObject> {
        val tile = getTileAt(worldX, worldY) ?: return emptyList()
        return tileUnits[tile.id]?.toList() ?: emptyList()
    }

    /**
     * 获取指定区块及其相邻区块的所有单位
     */
    fun getUnitsInTileAndNeighbors(tileId: Int): List<PlacedObject> {
        val result = mutableListOf<PlacedObject>()
        val tile = tiles.find { it.id == tileId } ?: return result

        for (dq in -1..1) {
            for (dr in -1..1) {
                val neighbor = tileMap[Pair(tile.q + dq, tile.r + dr)]
                if (neighbor != null) {
                    tileUnits[neighbor.id]?.let { result.addAll(it) }
                }
            }
        }
        return result
    }

    /**
     * 获取指定坐标所在区块及相邻区块的所有单位
     */
    fun getUnitsAtAndNeighbors(worldX: Float, worldY: Float): List<PlacedObject> {
        val tile = getTileAt(worldX, worldY) ?: return emptyList()
        return getUnitsInTileAndNeighbors(tile.id)
    }

    fun getAllTiles(): List<HexTile> = tiles

    fun getTileAt(worldX: Float, worldY: Float): HexTile? {
        if (!initialized) return null
        val q = ((worldX - worldCenterX) * sqrt(3.0) / 3f - (worldY - worldCenterY) / 3f) / hexSide
        val r = ((worldY - worldCenterY) * 2f / 3f) / hexSide
        return hexRound(q.toFloat(), r.toFloat())?.let { tileMap[it] }
    }

    fun getTileByAxial(q: Int, r: Int): HexTile? = tileMap[Pair(q, r)]

    fun getTileById(id: Int): HexTile? = tiles.find { it.id == id }

    fun isPointInHexGrid(worldX: Float, worldY: Float): Boolean {
        return getTileAt(worldX, worldY) != null
    }

    /**
     * 获取单位所在的区块ID
     */
    fun getTileIdForUnit(unit: PlacedObject): Int? = unitTile[unit]

    /**
     * 获取所有区块ID列表
     */
    fun getAllTileIds(): List<Int> = tiles.map { it.id }

    /**
     * 获取区块总数
     */
    fun getTileCount(): Int = tiles.size

    private fun hexRound(q: Float, r: Float): Pair<Int, Int>? {
        val s = -q - r
        var rq = q.roundToInt()
        var rr = r.roundToInt()
        val rs = s.roundToInt()

        val qDiff = abs(rq - q)
        val rDiff = abs(rr - r)
        val sDiff = abs(rs - s)

        if (qDiff > rDiff && qDiff > sDiff) {
            rq = -rr - rs
        } else if (rDiff > sDiff) {
            rr = -rq - rs
        }

        val absQ = abs(rq)
        val absR = abs(rr)
        val absSum = abs(rq + rr)
        if (absQ > maxLayer || absR > maxLayer || absSum > maxLayer) return null
        return Pair(rq, rr)
    }
}