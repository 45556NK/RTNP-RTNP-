package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import com.rtnp.demo.logic.DockingSystem
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

class DockingPointRenderer {

    private var shader: ShaderProgram? = null
    private var circleVertexBuffer: java.nio.FloatBuffer? = null
    private val circleSegments = 32

    // 空闲停泊点颜色 (r73,g126,b126,a170)
    private val freeColor = floatArrayOf(
        73f / 255f,
        126f / 255f,
        126f / 255f,
        170f / 255f
    )

    // 占用停泊点颜色 (r255,g52,b52,a150)
    private val occupiedColor = floatArrayOf(
        1f,
        52f / 255f,
        52f / 255f,
        150f / 255f
    )

    fun init() {
        val vert = """
            #version 300 es
            in vec2 aPosition;
            uniform mat4 uMVPMatrix;
            uniform vec2 uTranslation;
            uniform float uScale;
            void main() {
                vec2 pos = aPosition * uScale + uTranslation;
                gl_Position = uMVPMatrix * vec4(pos, 0.0, 1.0);
            }
        """.trimIndent()

        val frag = """
            #version 300 es
            precision mediump float;
            out vec4 fragColor;
            uniform vec4 uColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()

        shader = ShaderProgram.create(vert, frag)
            ?: throw RuntimeException("DockingPointRenderer shader failed")

        val verts = mutableListOf<Float>()
        verts.add(0f); verts.add(0f)
        for (i in 0..circleSegments) {
            val angle = 2.0 * Math.PI * i / circleSegments
            verts.add(cos(angle).toFloat())
            verts.add(sin(angle).toFloat())
        }
        circleVertexBuffer = ByteBuffer.allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(verts.toFloatArray())
            .apply { position(0) }
    }

    fun render(
        cameraMatrix: CameraMatrix,
        units: List<PlacedObject>,
        unitSystem: UnitSystem?,
        zoom: Float
    ) {
        if (unitSystem == null) return

        val selectedUnits = mutableSetOf<PlacedObject>()
        unitSystem.selectedUnit?.let { selectedUnits.add(it) }
        unitSystem.multiSelectManager?.let {
            if (it.isActive()) selectedUnits.addAll(it.getSelectedUnits())
        }
        if (selectedUnits.isEmpty()) return

        val freePoints = mutableListOf<Pair<Float, Float>>()
        val occupiedPoints = mutableListOf<Pair<Float, Float>>()

        val safeUnits = units.toList()
        for (obj in safeUnits) {
            if (DockingSystem.getSlotCount(obj) <= 0) continue
            if (obj.isDocked) continue

            val sectors = DockingSystem.getDockingSectors(obj)
            for (sector in sectors) {
                if (sector.isOccupied) {
                    occupiedPoints.add(Pair(sector.centerX, sector.centerY))
                } else {
                    freePoints.add(Pair(sector.centerX, sector.centerY))
                }
            }
        }

        if (freePoints.isEmpty() && occupiedPoints.isEmpty()) return

        // 保存状态
        val prevBlend = GLES30.glIsEnabled(GLES30.GL_BLEND)
        val prevDepth = GLES30.glIsEnabled(GLES30.GL_DEPTH_TEST)
        val prevStencil = GLES30.glIsEnabled(GLES30.GL_STENCIL_TEST)
        val prevLineWidth = FloatArray(1)
        GLES30.glGetFloatv(GLES30.GL_LINE_WIDTH, prevLineWidth, 0)

        val pointRadius = (5f / zoom).coerceIn(6f, 24f)

        val s = shader ?: return
        s.use()
        val buf = circleVertexBuffer ?: return
        buf.position(0)
        val posHandle = s.getAttribLocation("aPosition")
        if (posHandle == -1) return

        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(posHandle)

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)
        GLES30.glDisable(GLES30.GL_STENCIL_TEST)

        val mvp = cameraMatrix.getMVPMatrix()
        s.setMat4("uMVPMatrix", mvp)

        // 绘制空闲停泊点
        s.setVec4("uColor", freeColor)
        for ((x, y) in freePoints) {
            s.setVec2("uTranslation", floatArrayOf(x, y))
            s.setFloat("uScale", pointRadius)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 2 + circleSegments)
        }

        // 绘制占用停泊点
        s.setVec4("uColor", occupiedColor)
        for ((x, y) in occupiedPoints) {
            s.setVec2("uTranslation", floatArrayOf(x, y))
            s.setFloat("uScale", pointRadius)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 2 + circleSegments)
        }

        GLES30.glDisableVertexAttribArray(posHandle)

        // 恢复状态
        if (!prevBlend) GLES30.glDisable(GLES30.GL_BLEND)
        if (prevDepth) GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        if (prevStencil) GLES30.glEnable(GLES30.GL_STENCIL_TEST)
        GLES30.glDepthMask(true)
        GLES30.glLineWidth(prevLineWidth[0])
    }
}