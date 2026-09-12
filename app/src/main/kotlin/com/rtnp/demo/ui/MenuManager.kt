package com.rtnp.demo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import org.json.JSONObject
import java.util.*

class MenuManager(private val context: Context) {

    private val hexButtons = mutableListOf<HexButton>()
    private val directories = mutableMapOf<String, MutableList<JSONObject>>()
    private var currentDirectory: String? = null
    private var pressedButton: HexButton? = null

    @JvmField var screenWidth: Int = 0
    @JvmField var screenHeight: Int = 0

    fun getCurrentDirectory(): String? = currentDirectory
    
    /**
     * 获取所有按钮列表（供外部访问）
     */
    fun getHexButtons(): List<HexButton> = hexButtons

    fun reloadCurrentDirectory() {
        currentDirectory?.let { buildGrid() }
    }

    fun loadFromAssets(assetsPath: String) {
        try {
            context.assets.open(assetsPath).use { inputStream ->
                val buffer = ByteArray(inputStream.available())
                inputStream.read(buffer)
                val json = String(buffer, Charsets.UTF_8)
                val root = JSONObject(json)
                val base = root.getJSONObject("base_structure")

                val keys = base.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key.startsWith("directory_") || key.startsWith("menu_")) {
                        val dirObj = base.getJSONObject(key)
                        val components = dirObj.getJSONArray("components")
                        val compList = mutableListOf<JSONObject>()
                        for (i in 0 until components.length()) {
                            compList.add(components.getJSONObject(i))
                        }
                        directories[key] = compList
                    }
                }
                if (directories.isNotEmpty()) {
                    currentDirectory = directories.keys.first()
                    buildGrid()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildGrid() {
        hexButtons.clear()
        val components = directories[currentDirectory] ?: return

        val count = components.size
        val sideLength = 120f
        val borderWidth = 8f

        val coords = HexGridUtil.generateHexCoordsWithBorder(
            count, sideLength, borderWidth,
            screenWidth / 2f, screenHeight / 2f
        )

        for (i in 0 until count) {
            val comp = components[i]
            val pos = coords[i]
            val btn = HexButton(pos[0], pos[1], sideLength, comp.optString("label", ""))
            btn.strokeWidth = borderWidth

            val type = comp.optString("component_type", "text")
            when (type) {
                "button" -> {
                    btn.fillColor = Color.rgb(255, 165, 0)
                    btn.strokeColor = Color.WHITE
                    btn.textColor = Color.BLACK
                }
                "link" -> {
                    btn.fillColor = Color.rgb(100, 149, 237)
                    btn.strokeColor = Color.WHITE
                    btn.textColor = Color.BLACK
                    val target = comp.optString("target_directory")
                    if (target != null && target.isNotEmpty()) {
                        btn.setTargetDirectory(target)
                    }
                }
                else -> {
                    btn.fillColor = Color.rgb(200, 200, 200)
                    btn.strokeColor = Color.WHITE
                    btn.textColor = Color.BLACK
                }
            }
            hexButtons.add(btn)
        }
    }

    fun switchDirectory(dirId: String) {
        if (directories.containsKey(dirId)) {
            currentDirectory = dirId
            buildGrid()
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (hexButtons.isEmpty()) return false
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                for (btn in hexButtons) {
                    if (btn.isPointInside(x, y)) {
                        btn.onTouchDown()
                        pressedButton = btn
                        return true
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                pressedButton?.let {
                    if (!it.isPointInside(x, y)) {
                        it.cancel()
                        pressedButton = null
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                pressedButton?.let {
                    if (it.isPointInside(x, y)) {
                        it.onTouchUp()
                        it.performClick(this)
                    } else {
                        it.cancel()
                    }
                    pressedButton = null
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedButton?.cancel()
                pressedButton = null
            }
        }
        return true
    }

    fun draw(canvas: Canvas) {
        for (btn in hexButtons) {
            btn.draw(canvas)
        }
    }
}