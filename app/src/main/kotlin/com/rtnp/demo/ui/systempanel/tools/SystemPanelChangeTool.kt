package com.rtnp.demo.ui.systempanel.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.ui.FloatingWindowTool
import com.rtnp.demo.ui.HexButton
import com.rtnp.demo.ui.HexGridUtil
import com.rtnp.demo.ui.InfoBoxTool
import com.rtnp.demo.ui.systempanel.SystemPanelConstants
import kotlin.math.sqrt

object SystemPanelChangeTool {

    data class ChangeHexItem(
        val backgroundColor: Int,
        val label: String,
        val iconPath: String? = null
    )

    interface ChangeToolListener {
        fun onHexSelected(index: Int, centerX: Float, centerY: Float)
        fun onBottomButtonClicked(buttonType: BottomButtonType)
    }

    enum class BottomButtonType { CANCEL, CONFIRM }

    private var contentComponent: ChangeContentComponent? = null
    private var topInfoBoxId: Int = -1
    private var bottomInfoBoxId: Int = -1
    private var listener: ChangeToolListener? = null
    private var topInfoHeight: Float? = null

    @JvmStatic
    @JvmOverloads
    fun show(
        startX: Float,
        startY: Float,
        topText: String,
        hexItems: List<ChangeHexItem>,
        listener: ChangeToolListener,
        topInfoHeight: Float? = null
    ) {
        this.listener = listener
        this.topInfoHeight = topInfoHeight

        FloatingWindowTool.show(startX = startX, startY = startY)
        FloatingWindowTool.onOutsideClick = { hide() }

        val component = ChangeContentComponent(
            topText = topText,
            hexItems = hexItems,
            onHexSelected = { index, centerX, centerY ->
                listener.onHexSelected(index, centerX, centerY)
            }
        )
        contentComponent = component
        FloatingWindowTool.register(component)

        createTopInfoBox(topText)
        createBottomInfoBox()
        hideBottomConfirm()

        // ★ 初始无选中时隐藏顶部信息框
        setTopInfoVisible(topText.isNotEmpty())
    }

    @JvmStatic
    fun hide() {
        closeInfoBoxes()
        contentComponent?.let { FloatingWindowTool.unregister(it) }
        contentComponent = null
        FloatingWindowTool.hide()
        FloatingWindowTool.onOutsideClick = null
        listener = null
        topInfoHeight = null
    }

    @JvmStatic
    fun setTopText(text: String) {
        if (topInfoBoxId != -1) {
            InfoBoxTool.setText(topInfoBoxId, text)
        } else {
            createTopInfoBox(text)
        }
        setTopInfoVisible(text.isNotEmpty())
    }

    @JvmStatic
    fun setTopInfoVisible(visible: Boolean) {
        if (!visible) {
            if (topInfoBoxId != -1) {
                InfoBoxTool.hide(topInfoBoxId)
                topInfoBoxId = -1
            }
        } else if (topInfoBoxId == -1) {
            createTopInfoBox("")
        }
    }

    @JvmStatic
    fun setHexItems(items: List<ChangeHexItem>) {
        contentComponent?.updateHexItems(items)
    }

    @JvmStatic
    fun showBottomConfirm() {
        createBottomInfoBox()
    }

    @JvmStatic
    fun hideBottomConfirm() {
        if (bottomInfoBoxId != -1) {
            InfoBoxTool.hide(bottomInfoBoxId)
            bottomInfoBoxId = -1
        }
    }

    @JvmStatic
    fun resetSelection() {
        contentComponent?.resetSelection()
        setTopInfoVisible(false)
    }

    @JvmStatic
    fun getWindowRect(): RectF = FloatingWindowTool.getWindowRect()

    private fun createTopInfoBox(text: String) {
        val windowRect = FloatingWindowTool.getWindowRect()
        val gap = windowRect.width() / 20f
        val infoWidth = windowRect.width() - gap * 2f
        val infoHeight = topInfoHeight ?: (windowRect.height() / 4f)

        val infoLeft = windowRect.left + gap
        val infoTop = windowRect.top + gap

        if (topInfoBoxId != -1) InfoBoxTool.hide(topInfoBoxId)
        topInfoBoxId = InfoBoxTool.show(infoLeft, infoTop, infoWidth, infoHeight)
        InfoBoxTool.setText(topInfoBoxId, text)
    }

    private fun createBottomInfoBox() {
        val windowRect = FloatingWindowTool.getWindowRect()
        val gap = windowRect.width() / 20f
        val infoWidth = windowRect.width() - gap * 2f
        val infoHeight = windowRect.height() / 4f

        val infoLeft = windowRect.left + gap
        val infoTop = windowRect.bottom - infoHeight - gap

        if (bottomInfoBoxId != -1) InfoBoxTool.hide(bottomInfoBoxId)
        bottomInfoBoxId = InfoBoxTool.show(infoLeft, infoTop, infoWidth, infoHeight)

        InfoBoxTool.setText(bottomInfoBoxId, SystemPanelConstants.getText("confirm_replace"))

        val buttonDiameter = infoHeight / 3f
        val buttonGap = infoHeight / 20f
        val totalButtonsWidth = buttonDiameter * 2 + buttonGap
        val buttonsStartX = infoLeft + (infoWidth - totalButtonsWidth) / 2f
        val buttonsBottomY = infoTop + infoHeight - gap
        val buttonTopY = buttonsBottomY - buttonDiameter

        InfoBoxTool.addButton(
            taskId = bottomInfoBoxId,
            x = buttonsStartX - infoLeft,
            y = buttonTopY - infoTop,
            size = buttonDiameter,
            bgColor = Color.rgb(255, 182, 193),
            label = "取消",
            texturePath = "images/ui/panel_cancel_btn.png",
            iconScale = 0.8f
        )

        val confirmX = buttonsStartX + buttonDiameter + buttonGap
        InfoBoxTool.addButton(
            taskId = bottomInfoBoxId,
            x = confirmX - infoLeft,
            y = buttonTopY - infoTop,
            size = buttonDiameter,
            bgColor = Color.rgb(173, 216, 230),
            label = "更换",
            texturePath = "images/ui/panel_change_btn.png",
            iconScale = 0.8f
        )

        InfoBoxTool.onButtonClick = { taskId, buttonIndex ->
            if (taskId == bottomInfoBoxId) {
                when (buttonIndex) {
                    0 -> listener?.onBottomButtonClicked(BottomButtonType.CANCEL)
                    1 -> listener?.onBottomButtonClicked(BottomButtonType.CONFIRM)
                }
            }
        }
    }

    private fun closeInfoBoxes() {
        if (topInfoBoxId != -1) {
            InfoBoxTool.hide(topInfoBoxId)
            topInfoBoxId = -1
        }
        if (bottomInfoBoxId != -1) {
            InfoBoxTool.hide(bottomInfoBoxId)
            bottomInfoBoxId = -1
        }
        InfoBoxTool.onButtonClick = null
    }

    private class ChangeContentComponent(
        private var topText: String,
        private var hexItems: List<ChangeHexItem>,
        private val onHexSelected: (Int, Float, Float) -> Unit
    ) : FloatingWindowTool.FloatingWindowComponent {

        private val hexButtons = mutableListOf<HexButton>()
        private val hexButtonData = mutableListOf<HexButtonData>()

        private var selectedIndex = -1
        private var pressedButton: HexButton? = null
        private val lastWindowRect = RectF()

        // 缩放状态
        private var baseScale = 1f
        private var multiTouchStartDist = 0f
        private var multiTouchStartScale = 1f
        private var isMultiTouchActive = false

        private data class HexButtonData(
            val backgroundColor: Int,
            val label: String,
            val iconPath: String?,
            var bitmap: Bitmap? = null
        )

        fun updateHexItems(items: List<ChangeHexItem>) {
            hexItems = items
            selectedIndex = -1
            lastWindowRect.setEmpty()
        }

        fun resetSelection() {
            selectedIndex = -1
            updateSelectedAppearance()
            lastWindowRect.setEmpty()
        }

        override fun draw(canvas: Canvas, windowRect: RectF) {
            buildButtonsIfNeeded(windowRect)

            for (i in hexButtons.indices) {
                val btn = hexButtons[i]
                btn.draw(canvas)
                val data = hexButtonData.getOrNull(i) ?: continue
                data.bitmap?.let { bmp ->
                    val half = btn.size * 1.6f * 0.9f / 2f
                    val left = btn.centerX - half
                    val top = btn.centerY - half
                    canvas.drawBitmap(bmp, null, RectF(left, top, left + half * 2, top + half * 2), null)
                }
            }
        }

        override fun onTouchDown(x: Float, y: Float) {
            val screenX = x + lastWindowRect.left
            val screenY = y + lastWindowRect.top
            pressedButton = null
            for (btn in hexButtons) {
                if (btn.isPointInside(screenX, screenY)) {
                    btn.onTouchDown()
                    pressedButton = btn
                    return
                }
            }
        }

        override fun onTouchMove(x: Float, y: Float) {
            val screenX = x + lastWindowRect.left
            val screenY = y + lastWindowRect.top
            pressedButton?.let {
                if (!it.isPointInside(screenX, screenY)) {
                    it.cancel()
                    pressedButton = null
                }
            }
        }

        override fun onTouchUp(x: Float, y: Float) {
            val screenX = x + lastWindowRect.left
            val screenY = y + lastWindowRect.top
            pressedButton?.let { btn ->
                if (btn.isPointInside(screenX, screenY)) {
                    btn.onTouchUp()
                    val index = hexButtons.indexOf(btn)
                    if (index >= 0) {
                        selectedIndex = index
                        updateSelectedAppearance()
                        lastWindowRect.setEmpty()
                        val centerX = btn.centerX
                        val centerY = btn.centerY
                        onHexSelected(index, centerX, centerY)
                    }
                } else {
                    btn.cancel()
                }
                pressedButton = null
            }
        }

        override fun onMultiTouchStart(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {
            multiTouchStartDist = distance(firstX, firstY, secondX, secondY)
            multiTouchStartScale = baseScale
            isMultiTouchActive = true
        }

        override fun onMultiTouchMove(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {
            if (!isMultiTouchActive) return
            val currentDist = distance(firstX, firstY, secondX, secondY)
            if (multiTouchStartDist <= 0f) return
            val scaleFactor = currentDist / multiTouchStartDist
            val newScale = (multiTouchStartScale * scaleFactor).coerceIn(0.4f, 1.4f)
            baseScale = newScale
            lastWindowRect.setEmpty()
        }

        override fun onMultiTouchEnd() {
            isMultiTouchActive = false
        }

        private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val dx = x1 - x2
            val dy = y1 - y2
            return sqrt(dx * dx + dy * dy)
        }

        private fun updateSelectedAppearance() {
            for (i in hexButtons.indices) {
                val btn = hexButtons[i]
                btn.strokeColor = if (i == selectedIndex) Color.YELLOW else Color.WHITE
                btn.buildPaths()
            }
        }

        private fun buildButtonsIfNeeded(windowRect: RectF) {
            val sameRect = hexButtons.isNotEmpty() &&
                lastWindowRect.left == windowRect.left &&
                lastWindowRect.top == windowRect.top &&
                lastWindowRect.right == windowRect.right &&
                lastWindowRect.bottom == windowRect.bottom
            if (sameRect) return

            lastWindowRect.set(windowRect)

            hexButtons.clear()
            hexButtonData.clear()

            if (hexItems.isEmpty()) return

            val dynamicHexSize = windowRect.width() / 10f * baseScale
            val fontSize = GameConstants.getSmallFontSize(windowRect.height().toInt()) * baseScale
            val borderWidth = 8f * baseScale

            val coords = HexGridUtil.generateHexCoordsWithBorder(
                hexItems.size, dynamicHexSize, borderWidth,
                windowRect.centerX(), windowRect.centerY() + windowRect.height() * 0.1f
            )

            if (selectedIndex >= 0 && selectedIndex < coords.size) {
                val selectedCoord = coords[selectedIndex]
                val dx = windowRect.centerX() - selectedCoord[0]
                val dy = (windowRect.centerY() + windowRect.height() * 0.1f) - selectedCoord[1]
                for (coord in coords) {
                    coord[0] += dx
                    coord[1] += dy
                }
            }

            for (i in hexItems.indices) {
                val item = hexItems[i]
                val coord = coords[i]
                val btn = HexButton(coord[0], coord[1], dynamicHexSize, item.label)
                btn.fillColor = item.backgroundColor
                btn.strokeColor = if (i == selectedIndex) Color.YELLOW else Color.WHITE
                btn.pressedStrokeColor = Color.YELLOW
                btn.textColor = Color.BLACK
                btn.fontSize = fontSize
                btn.buildPaths()
                hexButtons.add(btn)

                val data = HexButtonData(item.backgroundColor, item.label, item.iconPath)
                if (item.iconPath != null) {
                    val context = FloatingWindowTool.getContext()
                    if (context != null) {
                        data.bitmap = ImageManager.getInstance(context).getBitmap(item.iconPath)
                    }
                }
                hexButtonData.add(data)
            }
        }
    }
}