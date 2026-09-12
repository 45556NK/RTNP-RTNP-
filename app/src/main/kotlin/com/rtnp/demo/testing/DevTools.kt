// app/src/main/kotlin/com/rtnp/demo/testing/DevTools.kt
package com.rtnp.demo.testing

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.text.InputType
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.testing.tools.DevToolFunction
import com.rtnp.demo.testing.tools.FunctionType
import com.rtnp.demo.testing.tools.MultiInputFunction
import com.rtnp.demo.testing.tools.SetSpeedMultiplierFunction
import dalvik.system.DexFile
import java.util.*
import kotlin.math.max
import kotlin.math.min

object DevTools {

    private var expanded = false
    private var screenW = 0
    private var screenH = 0

    private const val BUTTON_RADIUS = 56f
    private var buttonX = 0f
    private var buttonY = 0f

    // 按钮贴图（优先使用，没有则用小黄球）
    private var buttonBitmap: Bitmap? = null

    // 拖拽
    private var dragging = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var buttonDownTime = 0L

    // 面板滑动
    private var panelScrolling = false
    private var panelScrollLastY = 0f
    private var panelTouchDownTime = 0L
    private var panelTouchDownIndex = -1

    // 按钮按下状态
    private var pressedIndex = -1

    // 多条件输入面板
    private var multiInputPanel: MultiInputPanel? = null

    private const val TAP_MAX_DURATION = 300L

    interface OnPauseToggleListener {
        fun onPauseToggle(paused: Boolean)
    }
    var onPauseToggleListener: OnPauseToggleListener? = null

    private val panelWidth get() = screenW / 2f
    private val panelHeight get() = screenH / 3f
    private const val PANEL_MARGIN = 12f
    private val panelLeft: Float get() {
        val left = buttonX + BUTTON_RADIUS + PANEL_MARGIN
        return if (left + panelWidth > screenW) (screenW - panelWidth).coerceAtLeast(0f) else left
    }
    private val panelTop: Float get() {
        val top = buttonY - panelHeight / 2f
        return top.coerceIn(0f, screenH - panelHeight)
    }
    private const val PANEL_RADIUS = 16f

    private val functions = mutableListOf<DevToolFunction>()
    private val itemHeight: Int get() = (screenH / 15).coerceAtLeast(40)
    private val contentHeight: Int get() = functions.size * itemHeight
    private var scrollOffsetY = 0f
    private val maxScrollOffset: Float get() = max(0f, contentHeight - panelHeight + 10f)

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW; style = Paint.Style.FILL
    }
    private val buttonStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 4f
    }
    private val panelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 80, 80, 80); style = Paint.Style.FILL
    }
    private val panelBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2f
    }
    private val itemBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 120, 120, 120); style = Paint.Style.FILL
    }
    private val itemBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 1f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 26f
    }
    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; textSize = 22f; textAlign = Paint.Align.CENTER
    }

    private var initialized = false
    private var unitSystem: UnitSystem? = null
    private var activity: Activity? = null

    @JvmField var showPlacementGrid = false
    @JvmField var showUnitBounds = false

    fun init(us: UnitSystem, act: Activity) {
        if (initialized) return
        initialized = true
        unitSystem = us
        activity = act

        // 加载按钮贴图
        try {
            val img = ImageManager.getInstance(act)
            buttonBitmap = img.getBitmap("images/dev_btn.png")
        } catch (e: Exception) {
            buttonBitmap = null
        }

        loadFunctions()
    }

    private fun loadFunctions() {
        val us = unitSystem ?: return
        try {
            val dexFile = DexFile(us.context.packageCodePath)
            val entries: Enumeration<String> = dexFile.entries()
            val targetPackage = "com.rtnp.demo.testing.tools"
            while (entries.hasMoreElements()) {
                val className = entries.nextElement()
                if (!className.startsWith(targetPackage) || className.contains('$')) continue
                try {
                    val clazz = Class.forName(className)
                    if (!DevToolFunction::class.java.isAssignableFrom(clazz) ||
                        java.lang.reflect.Modifier.isAbstract(clazz.modifiers)) continue
                    val instance = try {
                        clazz.getDeclaredConstructor().newInstance() as DevToolFunction
                    } catch (e: NoSuchMethodException) {
                        clazz.getDeclaredConstructor(UnitSystem::class.java)
                            .newInstance(us) as DevToolFunction
                    }
                    functions.add(instance)
                } catch (e: Exception) {}
            }
            dexFile.close()
        } catch (e: Exception) { e.printStackTrace() }
        functions.sortBy { it.id }
    }

    fun updateScreenSize(w: Int, h: Int) {
        screenW = w; screenH = h
        buttonX = w / 2f; buttonY = h / 2f
        buttonX = buttonX.coerceIn(BUTTON_RADIUS, w - BUTTON_RADIUS)
        buttonY = buttonY.coerceIn(BUTTON_RADIUS, h - BUTTON_RADIUS)
        scrollOffsetY = scrollOffsetY.coerceIn(0f, maxScrollOffset)
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        // 多条件输入面板优先处理，直接接管所有触摸事件
        multiInputPanel?.let { panel ->
            return panel.onTouchEvent(event)
        }

        val x = event.x; val y = event.y
        if (expanded) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (isInsideButton(x, y)) {
                        dragging = true
                        dragOffsetX = buttonX - x
                        dragOffsetY = buttonY - y
                        buttonDownTime = System.currentTimeMillis()
                        return true
                    }
                    if (!isInsidePanel(x, y)) {
                        expanded = false
                        return true
                    }
                    panelScrolling = false
                    panelScrollLastY = y
                    panelTouchDownTime = System.currentTimeMillis()
                    val localY = y - panelTop + scrollOffsetY
                    panelTouchDownIndex = (localY / itemHeight).toInt()
                    pressedIndex = panelTouchDownIndex
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (dragging) {
                        buttonX = x + dragOffsetX
                        buttonY = y + dragOffsetY
                        buttonX = buttonX.coerceIn(BUTTON_RADIUS, screenW - BUTTON_RADIUS)
                        buttonY = buttonY.coerceIn(BUTTON_RADIUS, screenH - BUTTON_RADIUS)
                        return true
                    }
                    if (isInsidePanel(x, y)) {
                        panelScrolling = true
                        pressedIndex = -1
                        val dy = panelScrollLastY - y
                        scrollOffsetY += dy
                        scrollOffsetY = scrollOffsetY.coerceIn(0f, maxScrollOffset)
                        panelScrollLastY = y
                        return true
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        dragging = false
                        val duration = System.currentTimeMillis() - buttonDownTime
                        if (duration <= TAP_MAX_DURATION) {
                            expanded = false
                        }
                        return true
                    }
                    val duration = System.currentTimeMillis() - panelTouchDownTime
                    if (!panelScrolling && duration <= TAP_MAX_DURATION) {
                        val index = panelTouchDownIndex
                        if (index in functions.indices) {
                            val func = functions[index]
                            when {
                                func is MultiInputFunction -> openMultiInputPanel(func)
                                func.type == FunctionType.INPUT -> showInputDialog(func)
                                func.type == FunctionType.BUTTON || func.type == FunctionType.TOGGLE -> func.execute()
                            }
                        }
                    }
                    pressedIndex = -1
                    panelScrolling = false
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    panelScrolling = false
                    pressedIndex = -1
                    return true
                }
            }
            return true
        }

        // 面板未展开
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (isInsideButton(x, y)) {
                    dragging = true
                    dragOffsetX = buttonX - x
                    dragOffsetY = buttonY - y
                    buttonDownTime = System.currentTimeMillis()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    buttonX = x + dragOffsetX
                    buttonY = y + dragOffsetY
                    buttonX = buttonX.coerceIn(BUTTON_RADIUS, screenW - BUTTON_RADIUS)
                    buttonY = buttonY.coerceIn(BUTTON_RADIUS, screenH - BUTTON_RADIUS)
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (dragging) {
                    dragging = false
                    val duration = System.currentTimeMillis() - buttonDownTime
                    if (duration <= TAP_MAX_DURATION) {
                        expanded = !expanded
                    }
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> { dragging = false }
        }
        return false
    }

    private fun showInputDialog(func: DevToolFunction) {
        val act = activity ?: return
        val editText = EditText(act).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(func.getDisplayValue().replace("x", ""))
            selectAll()
        }

        val dialog = AlertDialog.Builder(act)
            .setTitle(func.name)
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val input = editText.text.toString()
                val value = input.toFloatOrNull() ?: 1.0f
                if (func is SetSpeedMultiplierFunction) {
                    func.applyValue(value)
                }
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.setOnShowListener {
            editText.requestFocus()
            val imm = act.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }
        dialog.show()
    }

    private fun openMultiInputPanel(func: MultiInputFunction) {
        val act = activity ?: return
        multiInputPanel = MultiInputPanel(
            act,
            screenW,
            screenH,
            func,
            onDismiss = { closeMultiInputPanel() },
            onNextFunction = { nextFunc -> openMultiInputPanel(nextFunc) }
        )
    }

    private fun closeMultiInputPanel() {
        multiInputPanel?.cleanup()
        multiInputPanel = null
    }

    fun draw(canvas: Canvas) {
        multiInputPanel?.let {
            it.draw(canvas)
            return
        }

        if (expanded) drawPanel(canvas)
        drawButton(canvas)
    }

    private fun drawPanel(canvas: Canvas) {
        val left = panelLeft; val top = panelTop
        val right = left + panelWidth; val bottom = top + panelHeight
        canvas.drawRoundRect(left, top, right, bottom, PANEL_RADIUS, PANEL_RADIUS, panelBgPaint)
        canvas.drawRoundRect(left, top, right, bottom, PANEL_RADIUS, PANEL_RADIUS, panelBorderPaint)
        canvas.save()
        canvas.clipRect(left + 4, top + 4, right - 4, bottom - 4)
        val startIndex = max(0, (scrollOffsetY / itemHeight).toInt())
        val endIndex = min(functions.size - 1, ((scrollOffsetY + panelHeight) / itemHeight).toInt())
        for (i in startIndex..endIndex) {
            val func = functions[i]
            val yPos = top - scrollOffsetY + i * itemHeight
            val rLeft = left + 4f; val rRight = right - 4f
            val rTop = yPos; val rBottom = yPos + itemHeight
            canvas.drawRect(rLeft, rTop, rRight, rBottom, itemBgPaint)
            canvas.drawRect(rLeft, rTop, rRight, rBottom, itemBorderPaint)

            val nameMaxWidth = rRight - rLeft - 140f
            val displayName = if (textPaint.measureText(func.name) > nameMaxWidth) {
                val tp = TextPaint().apply { set(textPaint) }
                TextUtils.ellipsize(func.name, tp, nameMaxWidth, TextUtils.TruncateAt.END).toString()
            } else {
                func.name
            }
            canvas.drawText(displayName, rLeft + 10f, rTop + itemHeight / 2f + 10f, textPaint)

            when (func.type) {
                FunctionType.BUTTON -> {
                    val btnW = 80f; val btnH = itemHeight * 0.7f
                    val btnLeft = rRight - btnW - 20f
                    val btnTop = rTop + (itemHeight - btnH) / 2f
                    val isPressed = (i == pressedIndex)
                    canvas.drawRect(btnLeft, btnTop, btnLeft + btnW, btnTop + btnH,
                        Paint().apply {
                            color = if (isPressed) Color.GRAY else Color.WHITE
                            style = Paint.Style.FILL
                        })
                    canvas.drawText("执行", btnLeft + btnW / 2f, btnTop + btnH / 2f + 8f, buttonTextPaint)
                }
                FunctionType.TOGGLE -> {
                    val switchW = 100f; val switchH = itemHeight * 0.75f
                    val switchLeft = rRight - switchW - 20f
                    val switchTop = rTop + (itemHeight - switchH) / 2f
                    val switchRight = switchLeft + switchW
                    val switchBottom = switchTop + switchH
                    val borderThickness = switchH / 6f
                    val isOn = func.getDisplayValue() == "ON"

                    canvas.drawRoundRect(
                        RectF(switchLeft, switchTop, switchRight, switchBottom),
                        switchH / 2f, switchH / 2f,
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = if (isOn) Color.WHITE else Color.GRAY
                            style = Paint.Style.FILL
                        })
                    canvas.drawRoundRect(
                        RectF(switchLeft, switchTop, switchRight, switchBottom),
                        switchH / 2f, switchH / 2f,
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.LTGRAY
                            style = Paint.Style.STROKE
                            strokeWidth = borderThickness
                        })
                    val sliderRadius = switchH / 2f - borderThickness
                    if (isOn) {
                        canvas.drawCircle(switchRight - sliderRadius - borderThickness,
                            switchTop + switchH / 2f, sliderRadius,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.WHITE; style = Paint.Style.FILL
                            })
                    } else {
                        canvas.drawCircle(switchLeft + sliderRadius + borderThickness,
                            switchTop + switchH / 2f, sliderRadius,
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.GRAY; style = Paint.Style.FILL
                            })
                    }
                }
                FunctionType.INPUT -> {
                    val inputW = 120f; val inputH = itemHeight * 0.7f
                    val inputLeft = rRight - inputW - 20f
                    val inputTop = rTop + (itemHeight - inputH) / 2f
                    canvas.drawRect(inputLeft, inputTop, inputLeft + inputW, inputTop + inputH,
                        Paint().apply { color = Color.DKGRAY; style = Paint.Style.FILL })
                    canvas.drawRect(inputLeft, inputTop, inputLeft + inputW, inputTop + inputH,
                        Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2f })
                    val valueText = func.getDisplayValue()
                    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.CYAN; textSize = 22f; textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(valueText, inputLeft + inputW / 2f,
                        inputTop + inputH / 2f + 8f, valuePaint)
                }
                else -> {}
            }
        }
        canvas.restore()
    }

    private fun drawButton(canvas: Canvas) {
        val bmp = buttonBitmap
        if (bmp != null && !bmp.isRecycled) {
            val size = BUTTON_RADIUS * 2
            val scale = min(size / bmp.width, size / bmp.height)
            val dw = bmp.width * scale
            val dh = bmp.height * scale
            val dst = RectF(buttonX - dw / 2, buttonY - dh / 2, buttonX + dw / 2, buttonY + dh / 2)
            canvas.drawBitmap(bmp, null, dst, null)
        } else {
            canvas.drawCircle(buttonX, buttonY, BUTTON_RADIUS, buttonPaint)
            canvas.drawCircle(buttonX, buttonY, BUTTON_RADIUS, buttonStrokePaint)
        }
    }

    private fun isInsideButton(x: Float, y: Float): Boolean {
        val dx = x - buttonX; val dy = y - buttonY
        return dx * dx + dy * dy <= BUTTON_RADIUS * BUTTON_RADIUS
    }

    private fun isInsidePanel(x: Float, y: Float): Boolean {
        if (!expanded) return false
        return x >= panelLeft && x <= panelLeft + panelWidth && y >= panelTop && y <= panelTop + panelHeight
    }
}