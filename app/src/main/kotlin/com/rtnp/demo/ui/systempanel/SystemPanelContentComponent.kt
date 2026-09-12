package com.rtnp.demo.ui.systempanel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.movement.weapon.WeaponManager
import com.rtnp.demo.strategy.StrategyManager
import com.rtnp.demo.ui.FloatingWindowTool
import com.rtnp.demo.ui.HexButton
import com.rtnp.demo.ui.HexGridUtil
import com.rtnp.demo.ui.InfoBoxTool
import com.rtnp.demo.ui.WarningMessage
import kotlin.math.sqrt
import kotlin.math.max

class SystemPanelContentComponent : FloatingWindowTool.FloatingWindowComponent {

    var unit: PlacedObject? = null
    var screenWidth: Int = 0
    var screenHeight: Int = 0

    private val hexButtons = mutableListOf<HexButton>()
    private val hexButtonData = mutableListOf<HexButtonData>()
    private var backButton: HexButton? = null
    private var pressedButton: HexButton? = null

    private val lastWindowRect = RectF()
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    private var baseScale = 1f
    private var multiTouchStartDist = 0f
    private var multiTouchStartScale = 1f
    private var isMultiTouchActive = false

    private var selectedSlotIndex = -1
    private var infoBoxId = -1

    private var changeButtonCenterX = 0f
    private var changeButtonCenterY = 0f

    // ★ 无槽位单位标志
    private var isEmptyUnit = false

    private data class HexButtonData(
        val type: String,
        val index: Int,
        val label: String,
        val bitmap: Bitmap?
    )

    override fun draw(canvas: Canvas, windowRect: RectF) {
        buildButtonsIfNeeded(windowRect)

        val titleText = SystemPanelConstants.getText("system_panel_title")
        val titleSize = windowRect.width() / 6f
        titlePaint.textSize = titleSize
        val titleY = windowRect.top + titleSize * 1.5f
        canvas.drawText(titleText, windowRect.centerX(), titleY, titlePaint)

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
        backButton?.draw(canvas)
    }

    override fun onTouchDown(x: Float, y: Float) {
        if (isEmptyUnit) {
            // ★ 无槽位单位：整个悬浮窗都可点击，直接关闭
            closeInfoBox()
            SystemPanelEntry.hide()
            return
        }

        val screenX = x + lastWindowRect.left
        val screenY = y + lastWindowRect.top
        pressedButton = null

        // 优先检测返回按钮
        backButton?.let {
            if (it.isPointInside(screenX, screenY)) {
                it.onTouchDown()
                pressedButton = it
                return
            }
        }

        for (btn in hexButtons) {
            if (btn.isPointInside(screenX, screenY)) {
                btn.onTouchDown()
                pressedButton = btn
                return
            }
        }
    }

    override fun onTouchMove(x: Float, y: Float) {
        if (isEmptyUnit) return
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
        if (isEmptyUnit) return
        val screenX = x + lastWindowRect.left
        val screenY = y + lastWindowRect.top
        pressedButton?.let { btn ->
            if (btn.isPointInside(screenX, screenY)) {
                btn.onTouchUp()
                when {
                    btn === backButton -> {
                        closeInfoBox()
                        SystemPanelEntry.hide()
                    }
                    else -> {
                        val index = hexButtons.indexOf(btn)
                        if (index >= 0 && index < hexButtonData.size) {
                            selectSlot(index)
                        }
                    }
                }
            } else {
                btn.cancel()
            }
            pressedButton = null
        }
    }

    override fun onMultiTouchStart(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {
        if (isEmptyUnit) return
        multiTouchStartDist = distance(firstX, firstY, secondX, secondY)
        multiTouchStartScale = baseScale
        isMultiTouchActive = true
    }

    override fun onMultiTouchMove(firstX: Float, firstY: Float, secondX: Float, secondY: Float) {
        if (isEmptyUnit) return
        if (!isMultiTouchActive) return
        val currentDist = distance(firstX, firstY, secondX, secondY)
        if (multiTouchStartDist <= 0f) return
        val scaleFactor = currentDist / multiTouchStartDist
        val newScale = (multiTouchStartScale * scaleFactor).coerceIn(0.5f, 1f)
        baseScale = newScale
        lastWindowRect.setEmpty()
    }

    override fun onMultiTouchEnd() {
        if (isEmptyUnit) return
        isMultiTouchActive = false
    }

    fun cleanup() {
        closeInfoBox()
    }

    private fun selectSlot(index: Int) {
        selectedSlotIndex = index
        lastWindowRect.setEmpty()
        updateInfoBox()
    }

    private fun resetToInitial() {
        baseScale = 1f
        selectedSlotIndex = -1
        lastWindowRect.setEmpty()
        closeInfoBox()
    }

    private fun updateInfoBox() {
        closeInfoBox()
        if (selectedSlotIndex < 0 || selectedSlotIndex >= hexButtonData.size) return

        val data = hexButtonData[selectedSlotIndex]
        val title = SystemPanelConstants.getText("change_component")
        val name = data.label

        val windowRect = FloatingWindowTool.getWindowRect()
        val gap = windowRect.height() / 20f
        val infoWidth = windowRect.width() - gap * 2f
        val infoHeight = windowRect.height() / 4f

        val infoLeft = windowRect.left + gap
        val infoTop = windowRect.bottom - infoHeight - gap

        infoBoxId = InfoBoxTool.show(infoLeft, infoTop, infoWidth, infoHeight)
        InfoBoxTool.setText(infoBoxId, "$title\n$name")

        val buttonDiameter = infoHeight / 3f
        val buttonGap = infoHeight / 20f
        val totalButtonsWidth = buttonDiameter * 2 + buttonGap
        val buttonsStartX = infoLeft + (infoWidth - totalButtonsWidth) / 2f
        val buttonsBottomY = infoTop + infoHeight - gap
        val buttonTopY = buttonsBottomY - buttonDiameter

        InfoBoxTool.addButton(
            taskId = infoBoxId,
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
            taskId = infoBoxId,
            x = confirmX - infoLeft,
            y = buttonTopY - infoTop,
            size = buttonDiameter,
            bgColor = Color.rgb(173, 216, 230),
            label = "更改",
            texturePath = "images/ui/panel_change_btn.png",
            iconScale = 0.8f
        )

        changeButtonCenterX = infoLeft + (confirmX - infoLeft) + buttonDiameter / 2f
        changeButtonCenterY = infoTop + (buttonTopY - infoTop) + buttonDiameter / 2f

        InfoBoxTool.onButtonClick = { taskId, buttonIndex ->
            if (taskId == infoBoxId) {
                when (buttonIndex) {
                    0 -> resetToInitial()
                    1 -> {
                        val selectedData = hexButtonData.getOrNull(selectedSlotIndex)
                        if (selectedData != null) {
                            val us = SystemPanelLogic.getUnitSystem()
                            val u = unit
                            if (us != null && u != null) {
                                val allowed = when (selectedData.type) {
                                    "weapon" -> u.allowWeaponChange
                                    "strategy" -> u.allowStrategyChange
                                    else -> true
                                }
                                if (allowed) {
                                    val slotIdx = selectedData.index
                                    closeInfoBox()
                                    SystemPanelEntry.hide()
                                    when (selectedData.type) {
                                        "weapon" -> WeaponChangeController.show(
                                            unitSystem = us,
                                            slotIndex = slotIdx,
                                            startX = changeButtonCenterX,
                                            startY = changeButtonCenterY,
                                            onFinished = {
                                                SystemPanelEntry.show(changeButtonCenterX, changeButtonCenterY, us)
                                            }
                                        )
                                        "strategy" -> StrategyChangeController.show(
                                            unitSystem = us,
                                            slotIndex = slotIdx,
                                            startX = changeButtonCenterX,
                                            startY = changeButtonCenterY,
                                            onFinished = {
                                                SystemPanelEntry.show(changeButtonCenterX, changeButtonCenterY, us)
                                            }
                                        )
                                    }
                                } else {
                                    WarningMessage.show("目标槽位已被固定！")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun closeInfoBox() {
        if (infoBoxId != -1) {
            InfoBoxTool.hide(infoBoxId)
            infoBoxId = -1
            InfoBoxTool.onButtonClick = null
        }
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }

    private fun buildButtonsIfNeeded(windowRect: RectF) {
        val sameRect = hexButtons.isNotEmpty() && backButton != null &&
            lastWindowRect.left == windowRect.left &&
            lastWindowRect.top == windowRect.top &&
            lastWindowRect.right == windowRect.right &&
            lastWindowRect.bottom == windowRect.bottom
        if (sameRect) return

        lastWindowRect.set(windowRect)

        hexButtons.clear()
        hexButtonData.clear()
        backButton = null
        isEmptyUnit = false

        val u = unit ?: return
        val context = FloatingWindowTool.getContext() ?: return

        val entries = mutableListOf<HexButtonData>()

        u.weaponSlots.forEachIndexed { index, slot ->
            val template = slot.templateId?.let { WeaponManager.getTemplate(it) }
            val label = if (slot.active) {
                template?.displayName ?: slot.type
            } else {
                SystemPanelConstants.getText("empty_weapon_slot")
            }
            val bitmap = template?.iconPath?.let { loadIcon(it, context) }
            entries.add(HexButtonData("weapon", index, label, bitmap))
        }

        val strategyIds = u.strategyIds
        for (i in 0 until u.strategySlots) {
            val sid = strategyIds.getOrNull(i) ?: ""
            val template = if (sid.isNotEmpty()) StrategyManager.getTemplate(sid) else null
            val label = if (sid.isEmpty()) {
                SystemPanelConstants.getText("empty_strategy_slot")
            } else {
                template?.displayName ?: sid
            }
            val bitmap = template?.iconPath?.let { loadIcon(it, context) }
            entries.add(HexButtonData("strategy", i, label, bitmap))
        }

        val hexSize = maxOf(SystemPanelConstants.getHexSize(screenWidth), windowRect.width() / 12f) * baseScale
        val fontSize = maxOf(SystemPanelConstants.getSmallFontSize(screenHeight), 24f) * baseScale
        val borderWidth = 8f * baseScale

        if (entries.isEmpty()) {
            isEmptyUnit = true
            backButton = HexButton(
                windowRect.centerX(), windowRect.centerY(), hexSize,
                SystemPanelConstants.getText("btn_back")
            ).apply {
                fillColor = Color.rgb(255, 165, 0)
                strokeColor = Color.WHITE
                pressedStrokeColor = Color.YELLOW
                textColor = Color.BLACK
                this.fontSize = fontSize
                buildPaths()
            }
            return
        }

        val total = entries.size + 1
        val coords = HexGridUtil.generateHexCoordsWithBorder(
            total, hexSize, borderWidth,
            windowRect.centerX(), windowRect.centerY()
        )

        if (selectedSlotIndex >= 0 && selectedSlotIndex < entries.size) {
            val selectedCoord = coords[selectedSlotIndex + 1]
            val dx = windowRect.centerX() - selectedCoord[0]
            val dy = windowRect.centerY() - selectedCoord[1]
            for (coord in coords) {
                coord[0] += dx
                coord[1] += dy
            }
        }

        backButton = HexButton(
            coords[0][0], coords[0][1], hexSize,
            SystemPanelConstants.getText("btn_back")
        ).apply {
            fillColor = Color.rgb(255, 165, 0)
            strokeColor = Color.WHITE
            pressedStrokeColor = Color.YELLOW
            textColor = Color.BLACK
            this.fontSize = fontSize
            buildPaths()
        }

        for (i in entries.indices) {
            val data = entries[i]
            val coord = coords[i + 1]
            val btn = HexButton(
                coord[0], coord[1], hexSize,
                if (data.bitmap == null) data.label else ""
            )
            btn.fillColor = if (data.type == "weapon") Color.rgb(255, 130, 130) else Color.rgb(190, 160, 255)
            btn.strokeColor = Color.WHITE
            btn.pressedStrokeColor = Color.YELLOW
            btn.textColor = Color.BLACK
            btn.fontSize = fontSize
            btn.buildPaths()
            hexButtons.add(btn)
            hexButtonData.add(data)
        }
    }

    private fun loadIcon(iconPath: String, context: android.content.Context): Bitmap? {
        return ImageManager.getInstance(context).getBitmap(iconPath)
    }
}