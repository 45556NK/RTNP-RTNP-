package com.rtnp.demo.gpu

import android.content.Context
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GameGLView(context: Context) : GLSurfaceView(context) {

    lateinit var renderer: GameRenderer

    init {
        // 设置 OpenGL ES 3.0 上下文
        setEGLContextClientVersion(3)

        // 设置透明背景（如果 UI 层需要穿透看到底层，可以保留透明，但测试阶段设为不透明蓝色）
        // 先简单设置一个颜色位深，稍后可以调整为透明
        setEGLConfigChooser(8, 8, 8, 8, 16, 8)  // 最后一位是 stencil 位数

        renderer = GameRenderer(context)
        setRenderer(renderer)

        // 渲染模式：连续渲染（后续可改为按需渲染）
        renderMode = RENDERMODE_CONTINUOUSLY
        // GameGLView.kt —— init 块中添加

        isClickable = false
        isFocusable = false
    }
}