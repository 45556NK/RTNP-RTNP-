// app/src/main/kotlin/com/rtnp/demo/strategy/TileSelectorTool.kt
package com.rtnp.demo.strategy

import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.ui.GameButton
import kotlin.math.abs

object TileSelectorTool {

    var isActive: Boolean = false
        private set

    var onTileConfirmed: ((HexGridManager.HexTile) -> Unit)? = null
    var onTileCancelled: (() -> Unit)? = null
    var onTileSelected: ((HexGridManager.HexTile) -> Unit)? = null
    var onInvalidTileSelected: ((HexGridManager.HexTile) -> Unit)? = null

    private var selectedTile: HexGridManager.HexTile? = null
    private var selectedTileRestricted = false

    var animStartTime: Long = 0L
        private set

    private var confirmButton: GameButton? = null
    private var cancelButton: GameButton? = null

    private var restrictedTiles: Set<Int>? = null
    private var rangeRestriction: Pair<Int, Int>? = null
    private var dynamicHost: PlacedObject? = null
    private var dynamicLayers: Int = 1
    private var lastDynamicTileId: Int = -1

    private var touchDownTime = 0L
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var hasMoved = false

    // ★ 按钮世界坐标
    private var worldToScreen: ((Float, Float) -> Pair<Float, Float>)? = null
    private var anchorWorldX: Float = 0f
    private var anchorWorldY: Float = 0f
    private var confirmOffsetX: Float = 0f
    private var confirmOffsetY: Float = 0f
    private var cancelOffsetX: Float = 0f
    private var cancelOffsetY: Float = 0f

    fun activate(screenWidth: Int, screenHeight: Int) {
        isActive = true
        selectedTile = null
        selectedTileRestricted = false
        restrictedTiles = null
        rangeRestriction = null
        dynamicHost = null
        dynamicLayers = 1
        lastDynamicTileId = -1
        onTileSelected = null
        onInvalidTileSelected = null
        onTileConfirmed = null
        onTileCancelled = null
        removeButtons()
    }

    fun deactivate() {
        isActive = false
        selectedTile = null
        selectedTileRestricted = false
        onTileSelected = null
        onInvalidTileSelected = null
        onTileConfirmed = null
        onTileCancelled = null
        removeButtons()
        dynamicHost = null
        lastDynamicTileId = -1
    }

    fun setCoordinateConverter(converter: (Float, Float) -> Pair<Float, Float>) {
        this.worldToScreen = converter
    }

    fun setRestrictedTiles(tileIds: Set<Int>) {
        restrictedTiles = tileIds
    }

    fun setRangeRestriction(centerTileId: Int, layers: Int) {
        rangeRestriction = Pair(centerTileId, layers)
    }

    fun setDynamicRangeRestriction(host: PlacedObject, layers: Int) {
        dynamicHost = host
        dynamicLayers = layers
        lastDynamicTileId = -1
        updateDynamicRestriction()
    }

    fun stopDynamicRangeRestriction() {
        dynamicHost = null
        lastDynamicTileId = -1
    }

    fun updateDynamicRestriction() {
        val host = dynamicHost ?: return
        val centerTile = HexGridManager.getTileAt(host.worldX, host.worldY)
        val centerId = centerTile?.id ?: -1

        if (centerId != lastDynamicTileId && centerId >= 0) {
            lastDynamicTileId = centerId
            restrictedTiles = setOf(centerId)
            selectedTile?.let {
                selectedTileRestricted = isTileRestricted(it.id)
                if (selectedTileRestricted) removeButtons() else createButtons()
            }
        }
    }

    fun getSelectedTile(): HexGridManager.HexTile? = selectedTile
    fun isSelectedTileRestricted(): Boolean = selectedTileRestricted

    private fun createButtons() {
        removeButtons()
        val tile = selectedTile ?: return
        if (selectedTileRestricted) return

        anchorWorldX = tile.worldX
        anchorWorldY = tile.worldY

        val btnSize = 80f
        val gap = 15f
        confirmOffsetX = -btnSize - gap
        confirmOffsetY = -btnSize / 2f
        cancelOffsetX = gap
        cancelOffsetY = -btnSize / 2f

        confirmButton = GameButton(0f, 0f, btnSize, btnSize, "确定").apply {
            setBackgroundColor(Color.rgb(34, 139, 34))
            setBorder(Color.WHITE, Color.YELLOW)
            setTextSize(24f)
            setOnClick { handleConfirm() }
        }
        cancelButton = GameButton(0f, 0f, btnSize, btnSize, "放弃").apply {
            setBackgroundColor(Color.rgb(200, 50, 50))
            setBorder(Color.WHITE, Color.YELLOW)
            setTextSize(24f)
            setOnClick { handleCancel() }
        }
    }

    private fun removeButtons() {
        confirmButton = null
        cancelButton = null
    }

    fun drawButtons(canvas: Canvas) {
        val converter = worldToScreen ?: return
        val (anchorSx, anchorSy) = converter(anchorWorldX, anchorWorldY)

        confirmButton?.setPosition(anchorSx + confirmOffsetX, anchorSy + confirmOffsetY)
        confirmButton?.draw(canvas)

        cancelButton?.setPosition(anchorSx + cancelOffsetX, anchorSy + cancelOffsetY)
        cancelButton?.draw(canvas)
    }

    fun onButtonTouch(x: Float, y: Float, action: Int): Boolean {
        val converter = worldToScreen ?: return false

        val (anchorSx, anchorSy) = converter(anchorWorldX, anchorWorldY)
        confirmButton?.setPosition(anchorSx + confirmOffsetX, anchorSy + confirmOffsetY)
        cancelButton?.setPosition(anchorSx + cancelOffsetX, anchorSy + cancelOffsetY)

        return when (action) {
            MotionEvent.ACTION_DOWN -> {
                confirmButton?.onTouchDown(x, y) == true || cancelButton?.onTouchDown(x, y) == true
            }
            MotionEvent.ACTION_UP -> {
                confirmButton?.onTouchUp(x, y) == true || cancelButton?.onTouchUp(x, y) == true
            }
            MotionEvent.ACTION_CANCEL -> {
                confirmButton?.cancel()
                cancelButton?.cancel()
                true
            }
            else -> false
        }
    }

    fun onTouchDown(screenX: Float, screenY: Float): Boolean {
        if (!isActive) return false
        touchDownTime = System.currentTimeMillis()
        touchDownX = screenX
        touchDownY = screenY
        hasMoved = false
        return true
    }

    fun onTouchMove(screenX: Float, screenY: Float) {
        if (!hasMoved) {
            val dx = screenX - touchDownX
            val dy = screenY - touchDownY
            if (dx * dx + dy * dy > 100f) hasMoved = true
        }
    }

    fun onTouchUp(screenX: Float, screenY: Float,
                  screenToWorld: (Float, Float) -> Pair<Float, Float>,
                  zoom: Float): Boolean {
        if (!isActive) return false
        if (hasMoved) return false
        if (System.currentTimeMillis() - touchDownTime > 800) return false

        val dx = screenX - touchDownX
        val dy = screenY - touchDownY
        if (dx * dx + dy * dy > 900f) return false

        try {
            val (wx, wy) = screenToWorld(screenX, screenY)
            val tile = HexGridManager.getTileAt(wx, wy) ?: return false

            selectedTile = tile
            animStartTime = System.currentTimeMillis()
            selectedTileRestricted = isTileRestricted(tile.id)

            if (selectedTileRestricted) {
                removeButtons()
                onInvalidTileSelected?.invoke(tile)
            } else {
                createButtons()
                onTileSelected?.invoke(tile)
            }
            return true
        } catch (_: Exception) {}
        return false
    }

    private fun handleConfirm() {
        val t = selectedTile ?: return
        removeButtons()
        onTileConfirmed?.invoke(t)
        if (!hasRestrictions() && dynamicHost == null) isActive = false
        selectedTile = null
        selectedTileRestricted = false
    }

    private fun handleCancel() {
        removeButtons()
        selectedTile = null
        selectedTileRestricted = false
        if (!hasRestrictions() && dynamicHost == null) {
            isActive = false
            onTileCancelled?.invoke()
        }
    }

    private fun hasRestrictions() = restrictedTiles != null || rangeRestriction != null

    private fun isTileRestricted(tileId: Int): Boolean {
        if (restrictedTiles != null && tileId in restrictedTiles!!) return true
        if (dynamicHost != null) {
            val centerId = HexGridManager.getTileAt(dynamicHost!!.worldX, dynamicHost!!.worldY)?.id ?: return true
            return !isTileInRange(tileId, centerId, dynamicLayers)
        }
        val (centerId, layers) = rangeRestriction ?: return false
        return !isTileInRange(tileId, centerId, layers)
    }

    private fun isTileInRange(tileId: Int, centerId: Int, layers: Int): Boolean {
        val tile = HexGridManager.getTileById(tileId) ?: return false
        val center = HexGridManager.getTileById(centerId) ?: return false
        val dq = abs(tile.q - center.q)
        val dr = abs(tile.r - center.r)
        val ds = abs((-tile.q - tile.r) - (-center.q - center.r))
        return (dq + dr + ds) / 2 <= layers
    }
}