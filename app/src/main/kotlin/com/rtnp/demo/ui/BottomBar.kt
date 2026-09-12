package com.rtnp.demo.ui

import android.content.Context
import android.graphics.*
import com.rtnp.demo.image.ImageManager

/**
 * 底部栏：包含暂停、建筑、舰队、物品按钮
 */
class BottomBar(private val context: Context) {

    interface OnToggleInventoryListener {
        fun onToggleInventory()
    }

    interface OnToggleBuildListener {
        fun onToggleBuild()
    }

    private var barHeight = 100
    private var btnSize = 90
    private var barLeft = 0
    private var barTop = 0
    private var barWidth = 0

    private val bgPaint: Paint = Paint().apply {
        color = Color.argb(200, 30, 30, 30)
    }

    private val borderPaint: Paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val textPaint: Paint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val btnPaint: Paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var invBtnLeft = 0
    private var invBtnTop = 0
    private var pauseBtnLeft = 0
    private var pauseBtnTop = 0
    private var buildBtnLeft = 0
    private var buildBtnTop = 0
    private var fleetBtnLeft = 0
    private var fleetBtnTop = 0

    private var isInventoryOpen = false
    private var isBuildOpen = false
    private var isFleetOpen = false

    private var pauseBitmap: Bitmap? = null
    private var buildBitmap: Bitmap? = null
    private var fleetBitmap: Bitmap? = null

    private val rect = Rect()

    private var inventoryListener: OnToggleInventoryListener? = null
    private var buildListener: OnToggleBuildListener? = null

    init {
        // 加载贴图 - 先获取 ImageManager 实例再调用 getBitmap
        try {
            val img = ImageManager.getInstance(context)
            pauseBitmap = img.getBitmap("images/pause_btn.png")
            buildBitmap = img.getBitmap("images/build_btn.png")
            fleetBitmap = img.getBitmap("images/fleet_btn.png")
        } catch (e: Exception) {
            pauseBitmap = null
            buildBitmap = null
            fleetBitmap = null
        }
    }

    fun setScreenSize(screenWidth: Int, screenHeight: Int) {
        barWidth = screenWidth / 2
        barLeft = screenWidth - barWidth
        barTop = screenHeight - barHeight

        val gap = 10
        pauseBtnLeft = barLeft + gap
        pauseBtnTop = barTop + (barHeight - btnSize) / 2
        buildBtnLeft = pauseBtnLeft + btnSize + gap
        buildBtnTop = pauseBtnTop
        fleetBtnLeft = buildBtnLeft + btnSize + gap
        fleetBtnTop = pauseBtnTop
        invBtnLeft = fleetBtnLeft + btnSize + gap
        invBtnTop = pauseBtnTop

        rect.set(barLeft, barTop, barLeft + barWidth, barTop + barHeight)
    }

    fun getRect(): Rect = rect

    fun setOnToggleInventoryListener(listener: OnToggleInventoryListener?) {
        inventoryListener = listener
    }

    fun setOnToggleBuildListener(listener: OnToggleBuildListener?) {
        buildListener = listener
    }

    fun setInventoryOpen(open: Boolean) {
        isInventoryOpen = open
    }

    fun setBuildOpen(open: Boolean) {
        isBuildOpen = open
    }

    fun setFleetOpen(open: Boolean) {
        isFleetOpen = open
    }

    fun contains(x: Float, y: Float): Boolean {
        return x >= barLeft && x <= barLeft + barWidth && y >= barTop && y <= barTop + barHeight
    }

    fun isInventoryButtonHit(x: Float, y: Float): Boolean {
        return x >= invBtnLeft && x <= invBtnLeft + btnSize && y >= invBtnTop && y <= invBtnTop + btnSize
    }

    fun isPauseButtonHit(x: Float, y: Float): Boolean {
        return x >= pauseBtnLeft && x <= pauseBtnLeft + btnSize && y >= pauseBtnTop && y <= pauseBtnTop + btnSize
    }

    fun isBuildButtonHit(x: Float, y: Float): Boolean {
        return x >= buildBtnLeft && x <= buildBtnLeft + btnSize && y >= buildBtnTop && y <= buildBtnTop + btnSize
    }

    fun isFleetButtonHit(x: Float, y: Float): Boolean {
        return x >= fleetBtnLeft && x <= fleetBtnLeft + btnSize && y >= fleetBtnTop && y <= fleetBtnTop + btnSize
    }

    fun draw(canvas: Canvas) {
        // 背景
        canvas.drawRect(barLeft.toFloat(), barTop.toFloat(),
                        (barLeft + barWidth).toFloat(), (barTop + barHeight).toFloat(), bgPaint)

        // --- 暂停按钮 ---
        val pBitmap = pauseBitmap
        if (pBitmap != null) {
            canvas.drawBitmap(pBitmap, null,
                RectF(pauseBtnLeft.toFloat(), pauseBtnTop.toFloat(),
                      (pauseBtnLeft + btnSize).toFloat(), (pauseBtnTop + btnSize).toFloat()), null)
        } else {
            btnPaint.color = Color.rgb(128, 0, 128)
            canvas.drawRect(pauseBtnLeft.toFloat(), pauseBtnTop.toFloat(),
                            (pauseBtnLeft + btnSize).toFloat(), (pauseBtnTop + btnSize).toFloat(), btnPaint)
            canvas.drawRect(pauseBtnLeft.toFloat(), pauseBtnTop.toFloat(),
                            (pauseBtnLeft + btnSize).toFloat(), (pauseBtnTop + btnSize).toFloat(), borderPaint)
            canvas.drawText("| |", pauseBtnLeft + btnSize / 2f, pauseBtnTop + btnSize / 2f + 10, textPaint)
        }

        // --- 建筑按钮 ---
        val bBitmap = buildBitmap
        if (bBitmap != null && !isBuildOpen) {
            canvas.drawBitmap(bBitmap, null,
                RectF(buildBtnLeft.toFloat(), buildBtnTop.toFloat(),
                      (buildBtnLeft + btnSize).toFloat(), (buildBtnTop + btnSize).toFloat()), null)
        } else {
            btnPaint.color = if (isBuildOpen) Color.RED else Color.rgb(128, 0, 128)
            canvas.drawRect(buildBtnLeft.toFloat(), buildBtnTop.toFloat(),
                            (buildBtnLeft + btnSize).toFloat(), (buildBtnTop + btnSize).toFloat(), btnPaint)
            canvas.drawRect(buildBtnLeft.toFloat(), buildBtnTop.toFloat(),
                            (buildBtnLeft + btnSize).toFloat(), (buildBtnTop + btnSize).toFloat(), borderPaint)
            val text = if (isBuildOpen) "×" else "B"
            canvas.drawText(text, buildBtnLeft + btnSize / 2f, buildBtnTop + btnSize / 2f + 12, textPaint)
        }

        // --- 舰队按钮 ---
        val fBitmap = fleetBitmap
        if (fBitmap != null && !isFleetOpen) {
            canvas.drawBitmap(fBitmap, null,
                RectF(fleetBtnLeft.toFloat(), fleetBtnTop.toFloat(),
                      (fleetBtnLeft + btnSize).toFloat(), (fleetBtnTop + btnSize).toFloat()), null)
        } else {
            btnPaint.color = if (isFleetOpen) Color.RED else Color.rgb(128, 0, 128)
            canvas.drawRect(fleetBtnLeft.toFloat(), fleetBtnTop.toFloat(),
                            (fleetBtnLeft + btnSize).toFloat(), (fleetBtnTop + btnSize).toFloat(), btnPaint)
            canvas.drawRect(fleetBtnLeft.toFloat(), fleetBtnTop.toFloat(),
                            (fleetBtnLeft + btnSize).toFloat(), (fleetBtnTop + btnSize).toFloat(), borderPaint)
            val text = if (isFleetOpen) "×" else "F"
            canvas.drawText(text, fleetBtnLeft + btnSize / 2f, fleetBtnTop + btnSize / 2f + 12, textPaint)
        }

        // --- 物品栏按钮 ---
        btnPaint.color = if (isInventoryOpen) Color.RED else Color.rgb(128, 0, 128)
        canvas.drawRect(invBtnLeft.toFloat(), invBtnTop.toFloat(),
                        (invBtnLeft + btnSize).toFloat(), (invBtnTop + btnSize).toFloat(), btnPaint)
        canvas.drawRect(invBtnLeft.toFloat(), invBtnTop.toFloat(),
                        (invBtnLeft + btnSize).toFloat(), (invBtnTop + btnSize).toFloat(), borderPaint)
        val text = if (isInventoryOpen) "×" else "T"
        canvas.drawText(text, invBtnLeft + btnSize / 2f, invBtnTop + btnSize / 2f + 12, textPaint)
    }
}