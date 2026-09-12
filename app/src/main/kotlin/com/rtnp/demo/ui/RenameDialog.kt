package com.rtnp.demo.ui

import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.rtnp.demo.core.PlacedObject

/**
 * 重命名对话框
 */
class RenameDialog(
    context: Context,
    private val currentName: String,
    private val allUnits: List<PlacedObject>,
    private val listener: OnRenameListener
) : Dialog(context) {

    interface OnRenameListener {
        fun onRenameConfirmed(newName: String)
    }

    private lateinit var editText: EditText
    private lateinit var errorText: TextView
    private val handler = Handler(Looper.getMainLooper())

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(createLayout(context))
        setCancelable(true)
        setCanceledOnTouchOutside(false)

        window?.apply {
            setLayout(600, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
            setGravity(Gravity.CENTER)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        }

        editText.setText(currentName)
        editText.setSelection(currentName.length)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateName(s.toString().trim())
            }
        })
    }

    private fun createLayout(context: Context): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 20, 30, 20)
            background = createBorderedBackground()
            isFocusable = true
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }

        val title = TextView(context).apply {
            text = "重命名"
            setTextColor(Color.WHITE)
            textSize = 24f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        root.addView(title)

        editText = EditText(context).apply {
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            textSize = 18f
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 15
            params.bottomMargin = 5
            layoutParams = params
        }
        root.addView(editText)

        errorText = TextView(context).apply {
            setTextColor(Color.RED)
            textSize = 14f
            visibility = View.GONE
        }
        root.addView(errorText)

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 15
            layoutParams = params
        }

        // 退出按钮
        val cancelBtn = HexConfirmView(context, "退出", Color.rgb(128, 128, 128), Color.BLACK)
        cancelBtn.setOnHexClickListener(object : HexConfirmView.OnHexClickListener {
            override fun onHexClick() {
                forceDismissWithBomb()
            }
        })
        buttonRow.addView(cancelBtn)

        val spacer = View(context)
        val spacerParams = LinearLayout.LayoutParams(30, 1)
        buttonRow.addView(spacer, spacerParams)

        // 确定按钮
        val confirmBtn = HexConfirmView(context, "确定", Color.rgb(255, 165, 0), Color.YELLOW)
        confirmBtn.setOnHexClickListener(object : HexConfirmView.OnHexClickListener {
            override fun onHexClick() {
                val newName = editText.text.toString().trim()
                if (newName.isEmpty()) {
                    errorText.text = "名称不能为空"
                    errorText.visibility = View.VISIBLE
                    return
                }
                if (isDuplicate(newName)) {
                    errorText.text = "已经有同名的单位！"
                    errorText.visibility = View.VISIBLE
                    return
                }
                forceDismissWithBomb()
                listener.onRenameConfirmed(newName)
            }
        })
        buttonRow.addView(confirmBtn)

        root.addView(buttonRow)
        return root
    }

    private fun forceDismissWithBomb() {
        dismissInternal()

        val bombRunnable = object : Runnable {
            override fun run() {
                if (isShowing) {
                    dismissInternal()
                    handler.postDelayed(this, 20)
                }
            }
        }
        handler.postDelayed(bombRunnable, 20)
    }

    private fun dismissInternal() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.let {
            window?.currentFocus?.let { view ->
                it.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }
        editText.clearFocus()
        super.dismiss()
    }

    override fun dismiss() {
        dismissInternal()
    }

    private fun isDuplicate(name: String): Boolean {
        if (name == currentName) return false
        for (obj in allUnits) {
            if (obj.displayName == name) return true
            if (obj.displayName == null && obj.name == name) return true
        }
        return false
    }

    private fun validateName(name: String) {
        if (name.isEmpty()) return
        if (isDuplicate(name)) {
            errorText.text = "已经有同名的单位！"
            errorText.visibility = View.VISIBLE
        } else {
            errorText.visibility = View.GONE
        }
    }

    private fun createBorderedBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.rgb(80, 80, 80))
            setStroke(5, Color.BLUE)
            cornerRadius = 10f
        }
    }

    // ======================= 自定义六边形按钮 =======================

    private class HexConfirmView(
        context: Context,
        label: String,
        fillColor: Int,
        pressStrokeColor: Int
    ) : View(context) {

        interface OnHexClickListener {
            fun onHexClick()
        }

        private val hexButton: HexButton
        private var hexClickListener: OnHexClickListener? = null
        private val normalStrokeColor = Color.WHITE
        private val pressedStrokeColor: Int
        private var pressed = false
        private var clickTriggered = false
        private val handler = Handler(Looper.getMainLooper())

        init {
            val size = 65f
            // 先创建临时对象，再设置属性
            hexButton = HexButton(size, size, size, label)
            hexButton.fillColor = fillColor
            hexButton.strokeColor = normalStrokeColor
            hexButton.fontSize = 38f
            hexButton.buildPaths()
            this.pressedStrokeColor = pressStrokeColor
            isFocusable = true
            isFocusableInTouchMode = true
        }

        fun setOnHexClickListener(listener: OnHexClickListener?) {
            hexClickListener = listener
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val size = (hexButton.size * 2 + 20).toInt()
            setMeasuredDimension(size, size)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            hexButton.centerX = cx
            hexButton.centerY = cy
            hexButton.buildPaths()
            hexButton.draw(canvas)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (isPointInside(x, y)) {
                        hexButton.strokeColor = pressedStrokeColor
                        invalidate()
                        hideKeyboardAndClearFocus()
                        parent.requestDisallowInterceptTouchEvent(true)
                        pressed = true

                        if (!clickTriggered) {
                            clickTriggered = true
                            handler.postDelayed({
                                hexClickListener?.onHexClick()
                                clickTriggered = false
                            }, 50)
                        }
                        return true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (pressed) {
                        hexButton.strokeColor = normalStrokeColor
                        invalidate()
                        pressed = false
                    }
                }
            }
            return true
        }

        private fun isPointInside(x: Float, y: Float): Boolean {
            hexButton.centerX = width / 2f
            hexButton.centerY = height / 2f
            hexButton.buildPaths()
            return hexButton.isPointInside(x, y)
        }

        private fun hideKeyboardAndClearFocus() {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.let {
                windowToken?.let { token ->
                    it.hideSoftInputFromWindow(token, 0)
                }
            }
            rootView?.findFocus()?.clearFocus()
        }
    }
}