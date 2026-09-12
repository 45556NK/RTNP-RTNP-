package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import android.util.Log
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

class RangeDisplaySystem {

    companion object {
        private const val RING_HALF_THICKNESS = 4f  // 圆环半厚度，总厚度 = 8
    }

    private var discShader: ShaderProgram? = null
    private var ringShader: ShaderProgram? = null
    private var minRangeDiscShader: ShaderProgram? = null  // 最小射程圆盘着色器
    
    private var discVertexBuffer: java.nio.FloatBuffer? = null
    private var ringVertexBuffer: java.nio.FloatBuffer? = null
    private var minRangeDiscVertexBuffer: java.nio.FloatBuffer? = null  // 最小射程圆盘顶点
    private val circleSegments = 512

    private val rangeCircles = mutableListOf<RangeCircle>()
    private val minRangeDiscs = mutableListOf<MinRangeDisc>()  // 最小射程圆盘数据
    private var lastSelectedUnitId: Long = -1L
    private var lastWeaponTemplateIds: List<String?> = emptyList()

    private val discColor = floatArrayOf(180f / 255f, 156f / 255f, 156f / 255f, 44f / 255f)
    private val ringColor = floatArrayOf(138f / 255f, 97f / 255f, 97f / 255f, 1.0f)
    // 最小射程圆盘颜色 (r172, g74, b74, a0.5)
    private val minRangeDiscColor = floatArrayOf(172f / 255f, 74f / 255f, 74f / 255f, 0.3f)

    fun init() {
        // 圆盘着色器
        val discVert = """
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

        val discFrag = """
            #version 300 es
            precision mediump float;
            out vec4 fragColor;
            uniform vec4 uColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()

        discShader = ShaderProgram.create(discVert, discFrag)

        // 圆环着色器（顶点包含半径缩放因子）
        val ringVert = """
            #version 300 es
            in vec2 aPosition;
            in float aRadiusScale;
            uniform mat4 uMVPMatrix;
            uniform vec2 uTranslation;
            uniform float uOuterRadius;
            uniform float uInnerRadius;
            void main() {
                float r = mix(uInnerRadius, uOuterRadius, aRadiusScale);
                vec2 pos = aPosition * r + uTranslation;
                gl_Position = uMVPMatrix * vec4(pos, 0.0, 1.0);
            }
        """.trimIndent()

        val ringFrag = """
            #version 300 es
            precision mediump float;
            out vec4 fragColor;
            uniform vec4 uColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()

        ringShader = ShaderProgram.create(ringVert, ringFrag)
        if (ringShader == null) {
            Log.e("RangeDisplay", "圆环着色器创建失败")
        } else {
            Log.d("RangeDisplay", "圆环着色器创建成功")
        }

        // 最小射程圆盘着色器（复用圆盘着色器程序）
        minRangeDiscShader = ShaderProgram.create(discVert, discFrag)
        if (minRangeDiscShader == null) {
            Log.e("RangeDisplay", "最小射程圆盘着色器创建失败")
        } else {
            Log.d("RangeDisplay", "最小射程圆盘着色器创建成功")
        }

        // 最大射程圆盘顶点
        val discVerts = mutableListOf<Float>()
        discVerts.add(0f); discVerts.add(0f)
        for (i in 0..circleSegments) {
            val angle = 2.0 * Math.PI * i / circleSegments
            discVerts.add(cos(angle).toFloat()); discVerts.add(sin(angle).toFloat())
        }
        discVertexBuffer = ByteBuffer.allocateDirect(discVerts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(discVerts.toFloatArray()).apply { position(0) }

        // 最小射程圆盘顶点（与最大射程圆盘顶点结构相同）
        val minRangeDiscVerts = mutableListOf<Float>()
        minRangeDiscVerts.add(0f); minRangeDiscVerts.add(0f)
        for (i in 0..circleSegments) {
            val angle = 2.0 * Math.PI * i / circleSegments
            minRangeDiscVerts.add(cos(angle).toFloat()); minRangeDiscVerts.add(sin(angle).toFloat())
        }
        minRangeDiscVertexBuffer = ByteBuffer.allocateDirect(minRangeDiscVerts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(minRangeDiscVerts.toFloatArray()).apply { position(0) }

        // 最大射程圆环顶点（交替外圈/内圈，aRadiusScale 区分：外圈=1.0，内圈=0.0）
        val ringVerts = mutableListOf<Float>()
        for (i in 0..circleSegments) {
            val angle = 2.0 * Math.PI * i / circleSegments
            val x = cos(angle).toFloat()
            val y = sin(angle).toFloat()
            // 外圈顶点：aRadiusScale = 1.0
            ringVerts.add(x); ringVerts.add(y); ringVerts.add(1.0f)
            // 内圈顶点：aRadiusScale = 0.0
            ringVerts.add(x); ringVerts.add(y); ringVerts.add(0.0f)
        }
        ringVertexBuffer = ByteBuffer.allocateDirect(ringVerts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(ringVerts.toFloatArray()).apply { position(0) }
    }

    fun update(unitSystem: UnitSystem?) {
        if (unitSystem == null) {
            rangeCircles.clear()
            minRangeDiscs.clear()
            lastSelectedUnitId = -1L
            lastWeaponTemplateIds = emptyList()
            return
        }

        val selectedUnit = unitSystem.selectedUnit
        val multiSelectManager = unitSystem.multiSelectManager

        if (selectedUnit == null) {
            rangeCircles.clear()
            minRangeDiscs.clear()
            lastSelectedUnitId = -1L
            lastWeaponTemplateIds = emptyList()
            return
        }

        if (multiSelectManager != null && multiSelectManager.isActive() && 
            multiSelectManager.getSelectedUnits().size > 1) {
            rangeCircles.clear()
            minRangeDiscs.clear()
            lastSelectedUnitId = -1L
            lastWeaponTemplateIds = emptyList()
            return
        }

        val currentTemplateIds = selectedUnit.weaponSlots.map { it.templateId }
        val weaponChanged = lastSelectedUnitId != selectedUnit.uniqueId ||
                           currentTemplateIds != lastWeaponTemplateIds

        if (weaponChanged) {
            // 重建最大射程圈
            rangeCircles.clear()
            for (ws in selectedUnit.weaponSlots) {
                if (ws.range > 0f) {
                    rangeCircles.add(
                        RangeCircle(
                            worldX = selectedUnit.worldX,
                            worldY = selectedUnit.worldY,
                            radius = ws.range,
                            discColor = discColor.clone(),
                            ringColor = ringColor.clone()
                        )
                    )
                }
            }
            
            // 重建最小射程圆盘
            minRangeDiscs.clear()
            for (ws in selectedUnit.weaponSlots) {
                if (ws.minRange > 0f) {
                    minRangeDiscs.add(
                        MinRangeDisc(
                            worldX = selectedUnit.worldX,
                            worldY = selectedUnit.worldY,
                            radius = ws.minRange,
                            discColor = minRangeDiscColor.clone()
                        )
                    )
                }
            }
            
            lastSelectedUnitId = selectedUnit.uniqueId
            lastWeaponTemplateIds = currentTemplateIds
        } else {
            // 更新位置
            for (circle in rangeCircles) {
                circle.worldX = selectedUnit.worldX
                circle.worldY = selectedUnit.worldY
            }
            for (disc in minRangeDiscs) {
                disc.worldX = selectedUnit.worldX
                disc.worldY = selectedUnit.worldY
            }
        }
    }

    fun render(cameraMatrix: CameraMatrix) {
        val mvp = cameraMatrix.getMVPMatrix()

        // 先渲染最小射程圆盘（在最大射程圆盘下方）
        minRangeDiscShader?.let { s ->
            if (minRangeDiscs.isEmpty()) return@let
            s.use()
            val vp = minRangeDiscVertexBuffer ?: return
            vp.position(0)
            val posHandle = s.getAttribLocation("aPosition")
            if (posHandle == -1) return
            
            GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, vp)
            GLES30.glEnableVertexAttribArray(posHandle)
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            s.setMat4("uMVPMatrix", mvp)
            
            for (disc in minRangeDiscs) {
                s.setVec4("uColor", disc.discColor)
                s.setFloat("uScale", disc.radius)
                s.setVec2("uTranslation", floatArrayOf(disc.worldX, disc.worldY))
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 2 + circleSegments)
            }
            GLES30.glDisableVertexAttribArray(posHandle)
        }

        // 渲染最大射程圆盘
        discShader?.let { s ->
            if (rangeCircles.isEmpty()) return@let
            s.use()
            val vp = discVertexBuffer ?: return
            vp.position(0)
            val posHandle = s.getAttribLocation("aPosition")
            if (posHandle == -1) return
            
            GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, vp)
            GLES30.glEnableVertexAttribArray(posHandle)
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            s.setMat4("uMVPMatrix", mvp)
            
            for (circle in rangeCircles) {
                s.setVec4("uColor", circle.discColor)
                s.setFloat("uScale", circle.radius)
                s.setVec2("uTranslation", floatArrayOf(circle.worldX, circle.worldY))
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 2 + circleSegments)
            }
            GLES30.glDisableVertexAttribArray(posHandle)
        }

        // 渲染最大射程圆环
        ringShader?.let { s ->
            if (rangeCircles.isEmpty()) return@let
            s.use()
            val vp = ringVertexBuffer ?: return
            vp.position(0)
            
            val stride = 3 * 4
            val posHandle = s.getAttribLocation("aPosition")
            val scaleHandle = s.getAttribLocation("aRadiusScale")
            
            if (posHandle == -1) return
            
            GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, stride, vp)
            GLES30.glEnableVertexAttribArray(posHandle)
            
            if (scaleHandle != -1) {
                vp.position(2)
                GLES30.glVertexAttribPointer(scaleHandle, 1, GLES30.GL_FLOAT, false, stride, vp)
                GLES30.glEnableVertexAttribArray(scaleHandle)
            }
            
            s.setMat4("uMVPMatrix", mvp)
            
            for (circle in rangeCircles) {
                s.setVec4("uColor", circle.ringColor)
                s.setVec2("uTranslation", floatArrayOf(circle.worldX, circle.worldY))
                s.setFloat("uOuterRadius", circle.radius + RING_HALF_THICKNESS)
                s.setFloat("uInnerRadius", circle.radius - RING_HALF_THICKNESS)
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, (circleSegments + 1) * 2)
            }
            
            GLES30.glDisableVertexAttribArray(posHandle)
            if (scaleHandle != -1) GLES30.glDisableVertexAttribArray(scaleHandle)
        }
    }

    data class RangeCircle(
        var worldX: Float,
        var worldY: Float,
        val radius: Float,
        val discColor: FloatArray,
        val ringColor: FloatArray
    )
    
    data class MinRangeDisc(
        var worldX: Float,
        var worldY: Float,
        val radius: Float,
        val discColor: FloatArray
    )
}