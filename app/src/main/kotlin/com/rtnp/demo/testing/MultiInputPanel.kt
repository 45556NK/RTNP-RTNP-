// app/src/main/kotlin/com/rtnp/demo/testing/MultiInputPanel.kt
package com.rtnp.demo.testing

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.rtnp.demo.testing.tools.InputField
import com.rtnp.demo.testing.tools.InputPage
import com.rtnp.demo.testing.tools.MultiInputFunction

class MultiInputPanel(
    private val context: Context,
    private val screenW: Int,
    private val screenH: Int,
    private val function: MultiInputFunction,
    private val onDismiss: () -> Unit,
    private val onNextFunction: ((MultiInputFunction) -> Unit)? = null
) {
    private val bgPaint = Paint().apply { color = Color.argb(200, 50, 50, 50) }
    private val panelBgPaint = Paint().apply { color = Color.argb(230, 40, 40, 40) }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 36f; textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 26f }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN; textSize = 26f; textAlign = Paint.Align.CENTER
    }
    private val inputBgPaint = Paint().apply { color = Color.argb(200, 20, 20, 20) }
    private val inputBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY; style = Paint.Style.STROKE; strokeWidth = 2f
    }
    private val inputBorderActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW; style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val buttonBgPaint = Paint().apply { color = Color.argb(200, 80, 80, 80) }
    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 30f; textAlign = Paint.Align.CENTER
    }
    private val focusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 0); style = Paint.Style.FILL
    }
    private val choiceIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 255, 255, 255); textSize = 20f; textAlign = Paint.Align.RIGHT
    }
    private val navButtonBgPaint = Paint().apply { color = Color.argb(200, 100, 100, 100) }

    private val pages: List<InputPage> = function.getPages().let { p ->
        if (p.isNotEmpty()) p
        else {
            val f = function.getInputFields()
            if (f.isNotEmpty()) listOf(InputPage(function.getTitle(), f)) else emptyList()
        }
    }
    private var currentPageIndex = 0
    private val pageValues = pages.map { p -> p.fields.map { it.defaultValue }.toMutableList() }
    private var focusedIndex = -1

    private val panelWidth: Int = (screenW * 0.85f).toInt()
    private val itemHeight: Int = (screenH / 20).coerceAtLeast(50)
    private val spacing = 15
    private val paddingH = 20
    private val paddingV = 15
    private val fieldHeight: Int = (itemHeight * 0.7f).toInt()
    private val navButtonHeight = 40

    // 固定面板高度（用最大字段数计算，整个生命周期不变）
    private val maxFieldCount = pages.maxOfOrNull { it.fields.size } ?: 1
    private val fixedContentHeight = paddingV * 2 + 50 + maxFieldCount * (itemHeight + spacing) + spacing + navButtonHeight + spacing + fieldHeight + spacing
    private val panelHeight: Int = fixedContentHeight.coerceAtMost(screenH - 60)

    private val panelLeft: Float = (screenW - panelWidth) / 2f
    private val panelTop: Float = ((screenH - panelHeight) / 2).toFloat()

    private var scrollOffsetY = 0f
    private var maxScrollY = 0f

    private var isDragging = false
    private var lastTouchY = 0f
    private var closeRequested = false

    private var hiddenEditText: EditText? = null

    private val curFields get() = pages[currentPageIndex].fields
    private val curValues get() = pageValues[currentPageIndex]

    init {
        val curContentH = paddingV * 2 + 50 + curFields.size * (itemHeight + spacing) + spacing + navButtonHeight + spacing + fieldHeight + spacing
        maxScrollY = (curContentH - panelHeight).toFloat().coerceAtLeast(0f)
        createHiddenEditText()
    }

    private fun createHiddenEditText() {
        try {
            hiddenEditText = EditText(context).apply {
                inputType = InputType.TYPE_CLASS_TEXT
                setTextColor(Color.TRANSPARENT)
                setBackgroundColor(Color.TRANSPARENT)
                setCursorVisible(false)
                visibility = View.INVISIBLE
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        if (focusedIndex in curFields.indices) curValues[focusedIndex] = s?.toString() ?: ""
                    }
                })
            }
            (context as? android.app.Activity)?.window?.decorView?.let { d ->
                if (d is android.view.ViewGroup) d.addView(hiddenEditText, android.view.ViewGroup.LayoutParams(1, 1))
            }
        } catch (_: Exception) {}
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x; val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = y
                isDragging = false
                closeRequested = false

                // ★ 面板外点击 → 关闭面板
                val insidePanel = x >= panelLeft && x <= panelLeft + panelWidth &&
                                y >= panelTop && y <= panelTop + panelHeight
                if (!insidePanel) {
                    return true
                }

                val halfW = (panelWidth - paddingH * 2 - spacing) / 2f

                // 导航按钮
                val navY = navButtonY() - scrollOffsetY
                if (currentPageIndex > 0 && x >= panelLeft + paddingH && x <= panelLeft + paddingH + halfW &&
                    y >= navY && y <= navY + navButtonHeight) {
                    goToPage(currentPageIndex - 1)
                    return true
                }
                if (currentPageIndex < pages.size - 1 && x >= panelLeft + paddingH + halfW + spacing &&
                    x <= panelLeft + panelWidth - paddingH && y >= navY && y <= navY + navButtonHeight) {
                    goToPage(currentPageIndex + 1)
                    return true
                }

                // 退出/完成按钮
                val btnY = buttonY() - scrollOffsetY
                if (x >= panelLeft + paddingH && x <= panelLeft + paddingH + halfW &&
                    y >= btnY && y <= btnY + fieldHeight) {
                    closeRequested = true
                    return true
                }
                if (currentPageIndex == pages.size - 1 && x >= panelLeft + paddingH + halfW + spacing &&
                    x <= panelLeft + panelWidth - paddingH && y >= btnY && y <= btnY + fieldHeight) {
                    val all = pageValues.flatten()
                    val next = function.onComplete(all)
                    if (next != null) onNextFunction?.invoke(next)
                    closeRequested = true
                    return true
                }

                // 输入框
                var fy = panelTop + paddingV + 50
                for (i in curFields.indices) {
                    val ft = fy - scrollOffsetY
                    val fb = ft + fieldHeight
                    val il = panelLeft + paddingH + (panelWidth - paddingH * 2) * 0.35f
                    val ir = panelLeft + panelWidth - paddingH
                    if (x >= il && x <= ir && y >= ft && y <= fb) {
                        val f = curFields[i]
                        if (f.choices != null && f.choices.isNotEmpty()) {
                            showChoicesDialog(i, f.choices)
                        } else {
                            focusedIndex = i
                            showKeyboard(i)
                        }
                        return true
                    }
                    fy += itemHeight + spacing
                }

                // 面板内空白 → 取消焦点
                focusedIndex = -1
                hideKeyboard()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (kotlin.math.abs(y - lastTouchY) > 10) isDragging = true
                if (isDragging) {
                    scrollOffsetY = (scrollOffsetY - (y - lastTouchY)).coerceIn(0f, maxScrollY)
                }
                lastTouchY = y
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (closeRequested) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post { onDismiss() }
                }
                isDragging = false
                closeRequested = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                closeRequested = false
                return true
            }
        }
        return true
    }

    private fun navButtonY(): Float = panelTop + paddingV + 50 + curFields.size * (itemHeight + spacing)
    private fun buttonY(): Float = navButtonY() + navButtonHeight + spacing

    private fun goToPage(i: Int) {
        currentPageIndex = i.coerceIn(0, pages.size - 1)
        focusedIndex = -1
        hideKeyboard()
        scrollOffsetY = 0f
        val h = paddingV * 2 + 50 + curFields.size * (itemHeight + spacing) + spacing + navButtonHeight + spacing + fieldHeight + spacing
        maxScrollY = (h - panelHeight).toFloat().coerceAtLeast(0f)
    }

    private fun showChoicesDialog(i: Int, choices: List<String>) {
        val act = context as? android.app.Activity ?: return
        AlertDialog.Builder(act)
            .setTitle(curFields[i].label)
            .setItems(choices.toTypedArray()) { _, w ->
                curValues[i] = choices[w]
                if (i + 1 < curFields.size) { focusedIndex = i + 1; showKeyboard(i + 1) }
            }
            .setNegativeButton("手动输入") { _, _ -> focusedIndex = i; showKeyboard(i) }
            .show()
    }

    private fun showKeyboard(i: Int) {
        hiddenEditText?.let { e ->
            curFields.getOrNull(i)?.let { e.inputType = it.inputType }
            e.setText(curValues.getOrElse(i) { "" })
            e.setSelection(e.text.length)
            e.visibility = View.VISIBLE
            e.requestFocus()
            e.postDelayed({
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInput(e, InputMethodManager.SHOW_IMPLICIT)
            }, 100)
        }
    }

    private fun hideKeyboard() {
        hiddenEditText?.let { e ->
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(e.windowToken, 0)
            e.clearFocus()
            e.visibility = View.INVISIBLE
        }
    }

    fun cleanup() {
        hideKeyboard()
        hiddenEditText?.let { (it.parent as? android.view.ViewGroup)?.removeView(it) }
        hiddenEditText = null
    }

    fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), bgPaint)
        canvas.drawRoundRect(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 16f, 16f, panelBgPaint)
        canvas.save()
        canvas.clipRect(panelLeft + 4, panelTop + 4, panelLeft + panelWidth - 4, panelTop + panelHeight - 4)

        var cy = panelTop + paddingV
        canvas.drawText(pages[currentPageIndex].title, panelLeft + panelWidth / 2f, cy + 40f, titlePaint)
        cy += 50

        for (i in curFields.indices) {
            val dy = cy - scrollOffsetY
            if (dy + fieldHeight > panelTop && dy < panelTop + panelHeight) {
                val il = panelLeft + paddingH + (panelWidth - paddingH * 2) * 0.35f
                val ir = panelLeft + panelWidth - paddingH
                canvas.drawText(curFields[i].label, panelLeft + paddingH, dy + fieldHeight * 0.65f, labelPaint)
                if (i == focusedIndex) canvas.drawRect(il, dy, ir, dy + fieldHeight, focusPaint)
                canvas.drawRect(il, dy, ir, dy + fieldHeight, inputBgPaint)
                canvas.drawRect(il, dy, ir, dy + fieldHeight, if (i == focusedIndex) inputBorderActivePaint else inputBorderPaint)
                val dv = curValues[i].takeIf { it.isNotEmpty() } ?: curFields[i].hint
                valuePaint.color = if (curValues[i].isEmpty()) Color.GRAY else Color.CYAN
                canvas.drawText(dv, (il + ir) / 2f, dy + fieldHeight * 0.65f, valuePaint)
                if (curFields[i].choices?.isNotEmpty() == true) canvas.drawText("▼", ir - 10f, dy + fieldHeight * 0.65f, choiceIndicatorPaint)
            }
            cy += itemHeight + spacing
        }

        val navY = cy - scrollOffsetY
        val halfW = (panelWidth - paddingH * 2 - spacing) / 2f
        if (pages.size > 1) {
            if (currentPageIndex > 0) {
                canvas.drawRect(panelLeft + paddingH, navY, panelLeft + paddingH + halfW, navY + navButtonHeight, navButtonBgPaint)
                canvas.drawRect(panelLeft + paddingH, navY, panelLeft + paddingH + halfW, navY + navButtonHeight, inputBorderPaint)
                canvas.drawText("上一页", panelLeft + paddingH + halfW / 2f, navY + navButtonHeight * 0.65f, buttonTextPaint)
            }
            if (currentPageIndex < pages.size - 1) {
                canvas.drawRect(panelLeft + paddingH + halfW + spacing, navY, panelLeft + panelWidth - paddingH, navY + navButtonHeight, navButtonBgPaint)
                canvas.drawRect(panelLeft + paddingH + halfW + spacing, navY, panelLeft + panelWidth - paddingH, navY + navButtonHeight, inputBorderPaint)
                canvas.drawText("下一页", panelLeft + paddingH + halfW + spacing + halfW / 2f, navY + navButtonHeight * 0.65f, buttonTextPaint)
            }
        }

        val btnY = navY + navButtonHeight + spacing
        canvas.drawRect(panelLeft + paddingH, btnY, panelLeft + paddingH + halfW, btnY + fieldHeight, buttonBgPaint)
        canvas.drawRect(panelLeft + paddingH, btnY, panelLeft + paddingH + halfW, btnY + fieldHeight, inputBorderPaint)
        canvas.drawText("退出", panelLeft + paddingH + halfW / 2f, btnY + fieldHeight * 0.65f, buttonTextPaint)

        if (currentPageIndex == pages.size - 1) {
            val cp = Paint().apply { color = Color.argb(200, 34, 139, 34); style = Paint.Style.FILL }
            canvas.drawRect(panelLeft + paddingH + halfW + spacing, btnY, panelLeft + panelWidth - paddingH, btnY + fieldHeight, cp)
            canvas.drawRect(panelLeft + paddingH + halfW + spacing, btnY, panelLeft + panelWidth - paddingH, btnY + fieldHeight, inputBorderPaint)
            canvas.drawText("完成", panelLeft + paddingH + halfW + spacing + halfW / 2f, btnY + fieldHeight * 0.65f, buttonTextPaint)
        }
        canvas.restore()
    }
}