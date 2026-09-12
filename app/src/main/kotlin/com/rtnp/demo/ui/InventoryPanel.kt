package com.rtnp.demo.ui

import android.graphics.*
import android.view.MotionEvent
import android.view.animation.PathInterpolator
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.MissileType
import com.rtnp.demo.core.MissileTypeRegistry
import com.rtnp.demo.movement.ModuleManager
import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.image.ImageManager
import android.content.Context

/**
 * 物品栏/建筑面板：显示可放置的物品列表，支持选择和详情查看
 */
class InventoryPanel(private val host: AnimationHost?, private val context: Context) {

    interface AnimationHost {
        fun requestRedraw()
    }

    interface OnItemSelectedListener {
        fun onItemSelected(item: InventoryItem)
        fun onItemDeselected()
    }

    private var panelLeft = 0
    private var panelTop = 0
    private var panelWidth = 0
    private var panelHeight = 0
    private val padding = 16
    private var columns = 2
    private var itemSize = 0
    private var rowHeight = 0

    private val items = mutableListOf<InventoryItem>()
    private var selectedIndex = -1
    private var scrollY = 0f
    private var maxScrollY = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val bgPaint: Paint = Paint().apply {
        color = Color.argb(200, 50, 50, 50)
    }
    private val itemBgPaint: Paint = Paint().apply {
        color = Color.argb(200, 0, 0, 0)
    }
    private val selectedPaint: Paint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val textPaint: Paint = Paint().apply {
        color = Color.WHITE
        textSize = 16f
        isAntiAlias = true
    }

    private var detailPanel = UnitDetailPanel()
    private var selectedUnitForDetail: PlacedObject? = null
    private var selectedItemForDetail: InventoryItem? = null

    private var isOpen = false
    private var animProgress = 0f
    private var isAnimating = false
    private val ANIM_DURATION = 300L
    private var animStartTime = 0L
    private val openInterpolator = PathInterpolator(0.05f, 0.7f, 0.1f, 1.0f)
    private val closeInterpolator = PathInterpolator(0.3f, 0.0f, 0.8f, 0.15f)

    private var screenHeight = 0
    private var listener: OnItemSelectedListener? = null
    private var isBuildPanel = false

    // 添加 setter
    fun setIsBuildPanel(value: Boolean) {
        isBuildPanel = value
    }

    init {
        detailPanel = UnitDetailPanel()
        items.clear()
        for (t in TemplateRegistry.templates) {
            if (items.none { it.name == t.name }) {
                items.add(t)
            }
        }
    }

    fun updateScreenSize(screenWidth: Int, screenHeight: Int) {
        this.screenHeight = screenHeight
        if (panelWidth != screenWidth / 2 || panelHeight != screenHeight / 2) {
            panelWidth = screenWidth / 2
            panelHeight = screenHeight / 2
            panelLeft = screenWidth - panelWidth
            panelTop = screenHeight - panelHeight
            itemSize = (panelWidth - padding * 3) / columns
            rowHeight = itemSize + padding
        }
    }

    fun getItems(): List<InventoryItem> = items

    fun setItems(newItems: List<InventoryItem>) {
        items.clear()
        items.addAll(newItems)
        deselectItem()
        scrollY = 0f
    }

    fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        this.listener = listener
    }

    fun getSelectedItem(): InventoryItem? {
        return if (selectedIndex in 0 until items.size) items[selectedIndex] else null
    }

    fun deselectItem() {
        selectedIndex = -1
        selectedItemForDetail = null
        listener?.onItemDeselected()
    }

    fun isAnimating(): Boolean = isAnimating
    fun isOpen(): Boolean = isOpen
    fun getAnimProgress(): Float = animProgress

    fun toggle() {
        if (isAnimating) return
        if (isOpen) deselectItem()
        isOpen = !isOpen
        isAnimating = true
        animStartTime = System.currentTimeMillis()
    
        // ★ 每次打开物品栏时，重新同步模板列表
        if (isOpen) {
            reloadTemplates()
        }
    
        host?.requestRedraw()
    }

    /**
    * 重新加载所有已注册的模板到物品栏
    */
    private fun reloadTemplates() {
        items.clear()
    
        if (isBuildPanel) {
            val buildItems = com.rtnp.demo.core.BuildListRegistry.getBuildTemplates()
            items.addAll(buildItems)
        } else {
            for (t in TemplateRegistry.templates) {
                if (items.none { it.name == t.name }) {
                    items.add(t)
                }
            }
        }
    }

    fun closeIfOpen() {
        if (isOpen && !isAnimating) toggle()
    }

    fun updateAnimation() {
        if (!isAnimating) return
        val elapsed = System.currentTimeMillis() - animStartTime
        val t = (elapsed / ANIM_DURATION.toFloat()).coerceAtMost(1f)
        animProgress = if (isOpen) {
            openInterpolator.getInterpolation(t)
        } else {
            1f - closeInterpolator.getInterpolation(t)
        }
        if (elapsed >= ANIM_DURATION) {
            animProgress = if (isOpen) 1f else 0f
            isAnimating = false
        } else {
            host?.requestRedraw()
        }
    }

    fun contains(x: Float, y: Float): Boolean {
        if (!isOpen || isAnimating) return false
        return x >= panelLeft && x <= panelLeft + panelWidth && y >= panelTop && y <= panelTop + panelHeight
    }

    fun setSelectedUnitForDetail(unit: PlacedObject?) {
        selectedUnitForDetail = unit
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (isAnimating || !isOpen) return false

        val x = event.x
        val y = event.y

        if (x < panelLeft || x > panelLeft + panelWidth || y < panelTop || y > panelTop + panelHeight) {
            isDragging = false
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = y
                isDragging = false
                val index = getItemIndexAt(x, y)
                if (index != -1) {
                    if (selectedIndex == index) deselectItem()
                    else selectItem(index)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = y - lastTouchY
                if (!isDragging && kotlin.math.abs(dy) > 10) isDragging = true
                if (isDragging) {
                    scrollY -= dy
                    scrollY = scrollY.coerceIn(0f, maxScrollY)
                }
                lastTouchY = y
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return true
    }

    private fun selectItem(index: Int) {
        selectedIndex = index
        selectedItemForDetail = items[index]
        listener?.onItemSelected(items[index])
    }

    private fun getItemIndexAt(x: Float, y: Float): Int {
        val totalWidth = columns * itemSize + (columns - 1) * padding
        val startX = panelLeft + (panelWidth - totalWidth) / 2
        val startY = panelTop + padding - scrollY.toInt()
        for (i in items.indices) {
            val row = i / columns
            val col = i % columns
            val itemX = startX + col * (itemSize + padding)
            val itemY = startY + row * rowHeight
            if (x >= itemX && x <= itemX + itemSize && y >= itemY && y <= itemY + itemSize) {
                return i
            }
        }
        return -1
    }

    fun draw(canvas: Canvas) {
        updateAnimation()
        if (animProgress <= 0f && !isAnimating) return

        val drawTop = screenHeight - panelHeight * animProgress
        canvas.save()
        canvas.clipRect(panelLeft.toFloat(), drawTop,
                        (panelLeft + panelWidth).toFloat(), drawTop + panelHeight)
        canvas.drawRect(panelLeft.toFloat(), drawTop,
                        (panelLeft + panelWidth).toFloat(), drawTop + panelHeight, bgPaint)

        val totalWidth = columns * itemSize + (columns - 1) * padding
        val startX = panelLeft + (panelWidth - totalWidth) / 2
        val startY = drawTop.toInt() + padding - scrollY.toInt()

        for (i in items.indices) {
            val row = i / columns
            val col = i % columns
            val itemX = startX + col * (itemSize + padding)
            val itemY = startY + row * rowHeight
            if (itemY + itemSize < drawTop || itemY > drawTop + panelHeight) continue
            drawItem(canvas, items[i], itemX, itemY, i == selectedIndex)
        }

        val totalRows = kotlin.math.ceil(items.size / columns.toFloat()).toInt()
        maxScrollY = maxOf(0f, (totalRows * rowHeight - panelHeight + padding * 2).toFloat())
        canvas.restore()
    }

    private fun drawItem(canvas: Canvas, item: InventoryItem, left: Int, top: Int, selected: Boolean) {
        canvas.drawRect(left.toFloat(), top.toFloat(),
                        (left + itemSize).toFloat(), (top + itemSize).toFloat(), itemBgPaint)
        val centerX = left + itemSize / 2
        val centerY = top + itemSize / 2

        val itemPaint = Paint().apply {
            color = item.color
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // ★ 贴图绘制（缩小到槽位的80%，保持宽高比并居中）
        val texturePath = item.texturePath
        if (item.useTexture && texturePath != null) {
            try {
                val bmp = ImageManager.getInstance(context).getBitmap(texturePath)
                if (bmp != null) {
                    val slotSize = itemSize.toFloat() * 0.8f  // 缩小到80%留边距
                    val bmpRatio = bmp.width.toFloat() / bmp.height
                    val drawW: Float
                    val drawH: Float
                    if (bmpRatio >= 1f) {
                        drawW = slotSize
                        drawH = slotSize / bmpRatio
                    } else {
                        drawH = slotSize
                        drawW = slotSize * bmpRatio
                    }

                    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
                    canvas.save()
                    canvas.translate(centerX.toFloat(), centerY.toFloat())
                    canvas.rotate(-90f) // 正方向默认北方
                    canvas.drawBitmap(bmp, null, RectF(-drawW / 2, -drawH / 2, drawW / 2, drawH / 2), bitmapPaint)
                    canvas.restore()
                } else {
                    drawFallbackShape(canvas, item, centerX, centerY, itemPaint)
                }
            } catch (e: Exception) {
                drawFallbackShape(canvas, item, centerX, centerY, itemPaint)
            }
        } else {
            drawFallbackShape(canvas, item, centerX, centerY, itemPaint)
        }

        // 选中边框
        if (selected) {
            canvas.drawRect(left + 2f, top + 2f, left + itemSize - 2f, top + itemSize - 2f, selectedPaint)
        }
    }

    private fun drawFallbackShape(canvas: Canvas, item: InventoryItem, cx: Int, cy: Int, paint: Paint) {
        when (item.shape) {
            "circle" -> canvas.drawCircle(cx.toFloat(), cy.toFloat(), item.size / 2f, paint)
            "square" -> {
                val half = item.size / 2f
                canvas.drawRect(cx - half, cy - half, cx + half, cy + half, paint)
            }
            "rectangle" -> {
                val halfW = item.width / 2f
                val halfH = item.height / 2f
                canvas.drawRect(cx - halfW, cy - halfH, cx + halfW, cy + halfH, paint)
            }
        }
    }

    companion object {
        private val templateMap = mutableMapOf<String, InventoryItem>()

        @JvmStatic
        fun registerTemplate(item: InventoryItem) {
            if (item.name != null) {
                templateMap[item.name] = item
            }
        }

        @JvmStatic
        fun getTemplateByName(name: String): InventoryItem? = templateMap[name]
    }
}