// app/src/main/kotlin/com/rtnp/demo/strategy/StrategyToolLayer.kt
package com.rtnp.demo.strategy

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.GameButton
import com.rtnp.demo.logger.Logger

object StrategyToolLayer {

    private const val CIRCLE_STROKE_WIDTH = 20f
    private const val MAX_TAP_DURATION = 300L

    var isActive: Boolean = false
        private set
    var radius: Float = 0f
        private set

    private var hostUnit: PlacedObject? = null

    var onTapInside: ((Float, Float) -> Unit)? = null
    var onTapOutside: (() -> Unit)? = null

    private var touchDownTime = 0L
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var hasMoved = false
    private var activePointerId = -1

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GameConstants.TOOL_CIRCLE_COLOR
        style = Paint.Style.STROKE
        strokeWidth = CIRCLE_STROKE_WIDTH
    }
    private val outOfRangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GameConstants.TOOL_OUT_OF_RANGE_COLOR
        style = Paint.Style.FILL
    }

    private var _staticCenterX: Float = 0f
    private var _staticCenterY: Float = 0f

    private val buttons = mutableListOf<GameButton>()
    private val buttonWorldPositions = mutableMapOf<GameButton, Pair<Float, Float>>()
    private val containers = mutableListOf<ButtonContainer>()

    private var worldToScreen: ((Float, Float) -> Pair<Float, Float>)? = null

    fun setCoordinateConverter(converter: (Float, Float) -> Pair<Float, Float>) {
        this.worldToScreen = converter
    }

    fun activate(host: PlacedObject, range: Float) {
        hostUnit = host
        radius = range
        isActive = true
        hasMoved = false
        activePointerId = -1
    }

    fun activate(worldX: Float, worldY: Float, range: Float) {
        hostUnit = null
        radius = range
        isActive = true
        _staticCenterX = worldX
        _staticCenterY = worldY
        hasMoved = false
        activePointerId = -1
    }

    fun deactivate() {
        isActive = false
        hostUnit = null
        onTapInside = null
        onTapOutside = null
        hasMoved = false
        activePointerId = -1
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isActive) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownTime = System.currentTimeMillis()
                touchDownX = event.x
                touchDownY = event.y
                hasMoved = false
                activePointerId = event.getPointerId(0)
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!hasMoved) {
                    val dx = event.x - touchDownX
                    val dy = event.y - touchDownY
                    if (dx * dx + dy * dy > 100f) {
                        hasMoved = true
                    }
                }
                return false
            }
            MotionEvent.ACTION_UP -> {
                if (activePointerId != event.getPointerId(0)) return false
                if (hasMoved) { activePointerId = -1; return false }
                val elapsed = System.currentTimeMillis() - touchDownTime
                if (elapsed > MAX_TAP_DURATION) { activePointerId = -1; return false }
                activePointerId = -1
                return false
            }
            MotionEvent.ACTION_CANCEL -> {
                activePointerId = -1
                hasMoved = false
                return false
            }
        }
        return false
    }

    fun isTapInside(worldX: Float, worldY: Float): Boolean {
        val (cx, cy) = getCurrentCenter()
        val dx = worldX - cx
        val dy = worldY - cy
        return dx * dx + dy * dy <= radius * radius
    }

    fun isTapInsideScreen(screenToWorld: (Float, Float) -> Pair<Float, Float>, screenX: Float, screenY: Float): Boolean {
        val (wx, wy) = screenToWorld(screenX, screenY)
        return isTapInside(wx, wy)
    }

    fun onTouchDown(screenX: Float, screenY: Float) {
        if (!isActive) return
        touchDownTime = System.currentTimeMillis()
        touchDownX = screenX
        touchDownY = screenY
    }

    fun onTouchUp(screenX: Float, screenY: Float,
                  screenToWorld: (Float, Float) -> Pair<Float, Float>): Boolean {
        if (!isActive) return false

        val elapsed = System.currentTimeMillis() - touchDownTime
        if (elapsed > MAX_TAP_DURATION) return true

        val dx = screenX - touchDownX
        val dy = screenY - touchDownY
        if (dx * dx + dy * dy > 100f) return true

        val (wx, wy) = screenToWorld(screenX, screenY)
        val (cx, cy) = getCurrentCenter()
        val distToCenter = Math.sqrt(
            ((wx - cx) * (wx - cx) + (wy - cy) * (wy - cy)).toDouble()
        ).toFloat()

        if (distToCenter <= radius) {
            onTapInside?.invoke(wx, wy)
        } else {
            onTapOutside?.invoke()
        }
        return true
    }

    fun draw(canvas: Canvas) {
        if (!isActive) return

        val (cx, cy) = getCurrentCenter()
        val converter = worldToScreen ?: return
        val (screenCx, screenCy) = converter(cx, cy)
        val edgeScreen = converter(cx + radius, cy)
        val screenRadius = Math.abs(edgeScreen.first - screenCx)

        canvas.drawCircle(screenCx, screenCy, screenRadius, circlePaint)

        canvas.save()
        val clipPath = Path().apply {
            addCircle(screenCx, screenCy, screenRadius, Path.Direction.CW)
        }
        canvas.clipOutPath(clipPath)
        canvas.drawRect(-50000f, -50000f, 50000f, 50000f, outOfRangePaint)
        canvas.restore()
    }

    fun drawStrategies(canvas: Canvas, allUnits: List<PlacedObject>) {
        StrategyManager.renderStrategies(canvas, allUnits)
    }

    fun addButton(button: GameButton, worldX: Float, worldY: Float) {
        buttons.add(button)
        buttonWorldPositions[button] = Pair(worldX, worldY)
    }

    fun removeButton(button: GameButton) {
        buttons.remove(button)
        buttonWorldPositions.remove(button)
    }

    fun createContainer(anchorWorldX: Float, anchorWorldY: Float): ButtonContainer {
        val container = ButtonContainer(anchorWorldX, anchorWorldY)
        containers.add(container)
        return container
    }

    fun removeContainer(container: ButtonContainer) {
        containers.remove(container)
    }

    fun onButtonTouch(x: Float, y: Float, action: Int): Boolean {
        for (btn in buttons) {
            when (action) {
                MotionEvent.ACTION_DOWN -> { if (btn.onTouchDown(x, y)) return true }
                MotionEvent.ACTION_UP -> { if (btn.onTouchUp(x, y)) return true }
                MotionEvent.ACTION_CANCEL -> btn.cancel()
            }
        }
        for (container in containers) {
            for (btn in container.buttons) {
                when (action) {
                    MotionEvent.ACTION_DOWN -> { if (btn.onTouchDown(x, y)) return true }
                    MotionEvent.ACTION_UP -> { if (btn.onTouchUp(x, y)) return true }
                    MotionEvent.ACTION_CANCEL -> btn.cancel()
                }
            }
        }
        return false
    }

    fun drawButtons(canvas: Canvas) {
        val converter = worldToScreen ?: return

        for (btn in buttons) {
            val worldPos = buttonWorldPositions[btn] ?: continue
            val (screenX, screenY) = converter(worldPos.first, worldPos.second)
            btn.setPosition(screenX, screenY)
            btn.draw(canvas)
        }

        for (container in containers) {
            val (anchorScreenX, anchorScreenY) = converter(container.anchorWorldX, container.anchorWorldY)
            for (btn in container.buttons) {
                val relPos = container.buttonRelativePositions[btn] ?: continue
                btn.setPosition(anchorScreenX + relPos.first, anchorScreenY + relPos.second)
                btn.draw(canvas)
            }
        }
    }

    private fun getCurrentCenter(): Pair<Float, Float> {
        val h = hostUnit
        return if (h != null) {
            Pair(h.worldX, h.worldY)
        } else {
            Pair(_staticCenterX, _staticCenterY)
        }
    }

    fun getCenterIfActive(): Pair<Float, Float>? {
        if (!isActive) return null
        return getCurrentCenter()
    }

    class ButtonContainer(
        val anchorWorldX: Float,
        val anchorWorldY: Float
    ) {
        val buttons = mutableListOf<GameButton>()
        val buttonRelativePositions = mutableMapOf<GameButton, Pair<Float, Float>>()

        fun addButton(button: GameButton, relativeX: Float, relativeY: Float) {
            buttons.add(button)
            buttonRelativePositions[button] = Pair(relativeX, relativeY)
        }

        fun removeButton(button: GameButton) {
            buttons.remove(button)
            buttonRelativePositions.remove(button)
        }

        fun clear() {
            buttons.clear()
            buttonRelativePositions.clear()
        }
    }
}