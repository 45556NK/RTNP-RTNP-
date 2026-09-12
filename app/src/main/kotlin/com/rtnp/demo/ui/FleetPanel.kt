package com.rtnp.demo.ui

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.image.ImageManager

class FleetPanel(context: Context) : View(context) {

    interface FleetPanelCallback {
        fun getFleetUnits(): List<PlacedObject>
        fun getSelectedUnit(): PlacedObject?
        fun onUnitSelected(unit: PlacedObject)
        fun isMultiSelectMode(): Boolean
        fun getSelectedUnits(): Set<PlacedObject>?
    }

    private var callback: FleetPanelCallback? = null

    private val PADDING_H = 12
    private val PADDING_V = 10
    private val ITEM_MARGIN = 20
    private val PIC_SIZE = 60
    private val TEXT_SIZE_TITLE = 22f
    private val TEXT_SIZE_TYPE = 18f
    private val TEXT_SIZE_HP = 20f
    private val LINE_HEIGHT = 26
    private val COLUMN_GAP = 8

    private val itemBgPaint: Paint = Paint().apply { color = Color.BLACK }
    private val titlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = TEXT_SIZE_TITLE }
    private val typePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = TEXT_SIZE_TYPE }
    private val hpPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED; textSize = TEXT_SIZE_HP }
    private val borderPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.rgb(173, 216, 230); strokeWidth = 5f }

    private val FILTER_NONE = 0
    private val FILTER_COMBAT = 1
    private val FILTER_ENGINEER = 2
    private val FILTER_BASE = 3

    private var currentFilter = FILTER_NONE
    private var filterBarRect: Rect? = null
    private var filterButtonRects: Array<Rect>? = null
    private val filterBitmaps: Array<Bitmap?> = arrayOfNulls(3)
    private val filterBorderPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
    private val filterBgPaint: Paint = Paint().apply { color = Color.argb(200, 60, 60, 60) }

    private var scrollY = 0f
    private var maxScrollY = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val layouts = mutableListOf<ItemLayout>()
    private var filteredUnits = mutableListOf<PlacedObject>()

    private data class ItemLayout(
        var unit: PlacedObject? = null,
        var itemTop: Int = 0,
        var itemHeight: Int = 0,
        var isVertical: Boolean = false,
        var titleLines: Array<String> = emptyArray(),
        var typeLines: Array<String> = emptyArray(),
        var hpLines: Array<String> = emptyArray()
    )

    init {
        setBackgroundColor(Color.argb(150, 80, 80, 80))
        try {
            val img = ImageManager.getInstance(context)
            filterBitmaps[0] = img.getBitmap("images/filter_combat_btn.png")
            filterBitmaps[1] = img.getBitmap("images/filter_engineer_btn.png")
            filterBitmaps[2] = img.getBitmap("images/filter_base_btn.png")
        } catch (e: Exception) {}
    }

    fun setCallback(callback: FleetPanelCallback?) {
        this.callback = callback
        refreshFilteredUnits()
    }

    fun refreshFilteredUnits() {
        val cb = callback ?: return
        val allUnits = cb.getFleetUnits() ?: emptyList()
        filteredUnits.clear()
        val seen = mutableSetOf<PlacedObject>()
        for (unit in allUnits) {
            if (seen.contains(unit)) continue
            seen.add(unit)
            if (unit.type != "unit" && unit.type != "base" && unit.type != "environment") continue
            when (currentFilter) {
                FILTER_COMBAT -> { if ("unit" == unit.type && unit.miningSpeed == 0f) filteredUnits.add(unit) }
                FILTER_ENGINEER -> { if ("unit" == unit.type && unit.miningSpeed > 0f) filteredUnits.add(unit) }
                FILTER_BASE -> { if ("base" == unit.type) filteredUnits.add(unit) }
                else -> filteredUnits.add(unit)
            }
        }
        measureLayouts()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val filterBarHeight = h / 20
        filterBarRect = Rect(0, 0, w, filterBarHeight)
        val btnWidth = w / 3
        filterButtonRects = Array(3) { i -> Rect(i * btnWidth, 0, (i + 1) * btnWidth, filterBarHeight) }
        measureLayouts()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) refreshFilteredUnits()
    }

    private fun measureLayouts() {
        layouts.clear()
        val units = filteredUnits
        if (units.isEmpty()) return
        val panelWidth = width
        if (panelWidth <= 0) return
        val contentWidth = panelWidth - PADDING_H * 2
        val halfWidth = contentWidth / 2
        var currentTop = (filterBarRect?.bottom ?: 0) + PADDING_V
        for (unit in units) {
            val layout = ItemLayout().apply { this.unit = unit }
            val displayName = unit.displayName ?: unit.name ?: "未知单位"
            val typeDesc = if (!unit.category.isNullOrEmpty()) "${unit.category} - ${unit.name ?: "未知"}" else unit.name ?: "未知"
            val hpText = "HP: ${unit.health}/${unit.originalHealth}"
            layout.titleLines = wrapText(displayName, titlePaint, 150f)
            layout.typeLines = wrapText(typeDesc, typePaint, 150f)
            layout.hpLines = wrapText(hpText, hpPaint, 150f)
            var rightMax = 0f; for (line in layout.typeLines) rightMax = maxOf(rightMax, typePaint.measureText(line))
            for (line in layout.hpLines) rightMax = maxOf(rightMax, hpPaint.measureText(line))
            var leftMax = 0f; for (line in layout.titleLines) leftMax = maxOf(leftMax, titlePaint.measureText(line))
            val vertical = (rightMax > halfWidth - COLUMN_GAP) || (leftMax > halfWidth - COLUMN_GAP)
            if (vertical) {
                layout.isVertical = true
                layout.itemHeight = PADDING_V * 2 + (layout.titleLines.size + layout.typeLines.size + layout.hpLines.size) * LINE_HEIGHT + 20 + PIC_SIZE + 10
            } else {
                layout.isVertical = false
                layout.itemHeight = maxOf(PADDING_V + PIC_SIZE + 5 + layout.titleLines.size * LINE_HEIGHT + PADDING_V, PADDING_V + layout.typeLines.size * LINE_HEIGHT + 5 + layout.hpLines.size * LINE_HEIGHT + PADDING_V)
            }
            layout.itemTop = currentTop
            currentTop += layout.itemHeight + ITEM_MARGIN
            layouts.add(layout)
        }
        if (layouts.isNotEmpty()) currentTop -= ITEM_MARGIN
        maxScrollY = maxOf(0f, (currentTop - height).toFloat())
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): Array<String> {
        if (text.isEmpty()) return arrayOf("")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        for (char in text) {
            val testLine = currentLine.toString() + char
            if (paint.measureText(testLine) <= maxWidth) { currentLine.append(char) }
            else { if (currentLine.isNotEmpty()) lines.add(currentLine.toString()); currentLine = StringBuilder(char.toString()) }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines.toTypedArray()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cb = callback ?: return
        canvas.save()
        filterBarRect?.let { canvas.clipRect(0f, it.bottom.toFloat(), width.toFloat(), height.toFloat()) }
        val currentSelected = cb.getSelectedUnit()
        for (layout in layouts) {
            val top = layout.itemTop - scrollY.toInt()
            if (top + layout.itemHeight < (filterBarRect?.bottom ?: 0) || top > height) continue
            drawItem(canvas, layout, top, currentSelected)
        }
        canvas.restore()
        val rect = filterBarRect ?: return
        canvas.drawRect(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat(), filterBgPaint)
        val btnRects = filterButtonRects ?: return
        for (i in 0 until 3) {
            val btnRect = btnRects[i]
            val bitmap = filterBitmaps[i]
            if (bitmap != null) canvas.drawBitmap(bitmap, null, RectF(btnRect), null)
            else {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 40f; textAlign = Paint.Align.CENTER }
                canvas.drawText(when (i) { 0 -> "战斗"; 1 -> "工程"; else -> "基地" }, btnRect.centerX().toFloat(), btnRect.centerY() + 15f, textPaint)
            }
            filterBorderPaint.color = if (currentFilter == i + 1) Color.CYAN else Color.TRANSPARENT
            canvas.drawRect(btnRect, filterBorderPaint)
        }
    }

    private fun drawItem(canvas: Canvas, layout: ItemLayout, top: Int, currentSelected: PlacedObject?) {
        val unit = layout.unit ?: return
        val left = PADDING_H; val right = width - PADDING_H
        val cardWidth = right - left; val cardHeight = layout.itemHeight
        canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), (top + cardHeight).toFloat(), itemBgPaint)
        val cb = callback ?: return
        val isHighlight = if (cb.isMultiSelectMode()) cb.getSelectedUnits()?.contains(unit) ?: false else currentSelected == unit
        if (isHighlight) canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), (top + cardHeight).toFloat(), borderPaint)
        if (layout.isVertical) {
            val centerX = left + cardWidth / 2
            var y = top + PADDING_V
            for (line in layout.titleLines) { canvas.drawText(line, centerX - titlePaint.measureText(line) / 2, y + titlePaint.textSize, titlePaint); y += LINE_HEIGHT }
            for (line in layout.typeLines) { canvas.drawText(line, centerX - typePaint.measureText(line) / 2, y + typePaint.textSize, typePaint); y += LINE_HEIGHT }
            drawUnitIcon(canvas, unit, centerX - PIC_SIZE / 2, y, PIC_SIZE); y += PIC_SIZE + 10
            for (line in layout.hpLines) { canvas.drawText(line, centerX - hpPaint.measureText(line) / 2, y + hpPaint.textSize, hpPaint); y += LINE_HEIGHT }
        } else {
            drawUnitIcon(canvas, unit, left + PADDING_H, top + PADDING_V, PIC_SIZE)
            var nameY = top + PADDING_V + PIC_SIZE + 5
            for (line in layout.titleLines) { canvas.drawText(line, (left + PADDING_H).toFloat(), nameY + titlePaint.textSize, titlePaint); nameY += LINE_HEIGHT }
            var typeY = top + PADDING_V
            for (line in layout.typeLines) { val w = typePaint.measureText(line); canvas.drawText(line, (right - PADDING_H) - w, typeY + typePaint.textSize, typePaint); typeY += LINE_HEIGHT }
            var hpY = typeY + 5
            for (line in layout.hpLines) { val w = hpPaint.measureText(line); canvas.drawText(line, (right - PADDING_H) - w, hpY + hpPaint.textSize, hpPaint); hpY += LINE_HEIGHT }
        }
    }

    private fun drawUnitIcon(canvas: Canvas, unit: PlacedObject, left: Int, top: Int, size: Int) {
        val bmp = unit.textureBitmap
        if (bmp != null && !bmp.isRecycled) {
            val bmpW = bmp.width.toFloat(); val bmpH = bmp.height.toFloat()
            val scale = minOf(size / bmpW, size / bmpH); val drawW = bmpW * scale; val drawH = bmpH * scale
            canvas.save(); canvas.translate(left + size / 2f, top + size / 2f)
            canvas.drawBitmap(bmp, null, RectF(-drawW / 2, -drawH / 2, drawW / 2, drawH / 2), null)
            canvas.restore()
        } else {
            val shapePaint = Paint().apply { color = unit.color; style = Paint.Style.FILL }
            val cx = left + size / 2f; val cy = top + size / 2f
            when (unit.shape) {
                "circle" -> canvas.drawCircle(cx, cy, size / 2f, shapePaint)
                "square" -> { val half = unit.size / 2f; canvas.drawRect(cx - half, cy - half, cx + half, cy + half, shapePaint) }
                else -> { val r = unit.height.toFloat() / unit.width.toFloat(); var dw = size.toFloat(); var dh = size * r; if (dh > size) { dh = size.toFloat(); dw = size / r }; canvas.drawRect(cx - dw / 2, cy - dh / 2, cx + dw / 2, cy + dh / 2, shapePaint) }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val cb = callback ?: return true
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastTouchY = y; isDragging = false; return true }
            MotionEvent.ACTION_MOVE -> {
                val dy = y - lastTouchY
                if (!isDragging && kotlin.math.abs(dy) > 10) isDragging = true
                if (isDragging) { scrollY = (scrollY - dy).coerceIn(0f, maxScrollY); invalidate() }
                lastTouchY = y; return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    val x = event.x
                    val btnRects = filterButtonRects
                    if (btnRects != null) {
                        for (i in 0 until 3) {
                            if (btnRects[i].contains(x.toInt(), y.toInt())) {
                                currentFilter = if (currentFilter == i + 1) FILTER_NONE else i + 1
                                refreshFilteredUnits(); return true
                            }
                        }
                    }
                    // ★ 通过坐标找行号，直接用 filteredUnits 索引取单位
                    measureLayouts()
                    val clickY = y + scrollY
                    var clickedIndex = -1
                    for (i in layouts.indices) {
                        if (clickY >= layouts[i].itemTop && clickY <= layouts[i].itemTop + layouts[i].itemHeight) {
                            clickedIndex = i
                            break
                        }
                    }
                    if (clickedIndex >= 0 && clickedIndex < filteredUnits.size) {
                        cb.onUnitSelected(filteredUnits[clickedIndex])
                    }
                }
                isDragging = false; return true
            }
            MotionEvent.ACTION_CANCEL -> { isDragging = false; return true }
        }
        return false
    }
}