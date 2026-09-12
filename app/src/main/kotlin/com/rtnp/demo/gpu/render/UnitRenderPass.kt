package com.rtnp.demo.gpu.render

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import android.opengl.Matrix
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import com.rtnp.demo.gpu.effect.EffectLayer
import com.rtnp.demo.gpu.effect.GpuEffectInstance
import com.rtnp.demo.gpu.effect.GpuEffectSystem
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

class UnitRenderPass {

    internal var texturedShader: ShaderProgram? = null
    internal var colorShader: ShaderProgram? = null
    private val textureCache = mutableMapOf<Bitmap, Int>()

    private val rectVertices = floatArrayOf(
        -0.5f,  0.5f, 0f,
        -0.5f, -0.5f, 0f,
         0.5f,  0.5f, 0f,
         0.5f, -0.5f, 0f
    )
    private val rectBuffer: FloatBuffer = ByteBuffer.allocateDirect(rectVertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(rectVertices)
        .apply { position(0) }

    private val circleVertices: FloatArray
    private val circleBuffer: FloatBuffer

    private val texCoords = floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 0f,
        1f, 1f
    )
    private val texCoordBuffer: FloatBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(texCoords)
        .apply { position(0) }

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    init {
        val segments = 32
        val verts = mutableListOf<Float>()
        for (i in 0..segments) {
            val angle = 2.0 * Math.PI * i / segments
            verts.add(0f)
            verts.add(0f)
            verts.add(0f)
            verts.add((cos(angle) * 0.5f).toFloat())
            verts.add((sin(angle) * 0.5f).toFloat())
            verts.add(0f)
        }
        circleVertices = verts.toFloatArray()
        circleBuffer = ByteBuffer.allocateDirect(circleVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(circleVertices)
            .apply { position(0) }
    }

    fun init() {
        val colorVert = """
            #version 300 es
            layout(location = 0) in vec4 aPosition;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
            }
        """.trimIndent()
        val colorFrag = """
            #version 300 es
            precision mediump float;
            out vec4 fragColor;
            uniform vec4 uColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()
        colorShader = ShaderProgram.create(colorVert, colorFrag)
            ?: throw RuntimeException("Failed color shader")

        val texVert = """
            #version 300 es
            layout(location = 0) in vec4 aPosition;
            layout(location = 1) in vec2 aTexCoord;
            out vec2 vTexCoord;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()
        val texFrag = """
            #version 300 es
            precision mediump float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            uniform sampler2D uTexture;
            void main() {
                fragColor = texture(uTexture, vTexCoord);
            }
        """.trimIndent()
        texturedShader = ShaderProgram.create(texVert, texFrag)
            ?: throw RuntimeException("Failed texture shader")

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun draw(units: List<PlacedObject>, cameraMatrix: CameraMatrix) {
        val snapshot = units.toList()
        val mvp = cameraMatrix.getMVPMatrix()

        // 按类型分组
        val environments = mutableListOf<PlacedObject>()
        val bases = mutableListOf<PlacedObject>()
        val normalUnits = mutableListOf<PlacedObject>()

        for (unit in snapshot) {
            if (unit.category == "建造中单位") continue
            when (unit.type) {
                "environment" -> environments.add(unit)
                "base" -> bases.add(unit)
                else -> normalUnits.add(unit)
            }
        }

        // 第1层：环境单位
        for (unit in environments) {
            drawUnitWithEffects(unit, mvp, cameraMatrix)
        }

        // 第2层：基地
        for (unit in bases) {
            drawUnitWithEffects(unit, mvp, cameraMatrix)
        }

        // 第3层：普通单位
        for (unit in normalUnits) {
            drawUnitWithEffects(unit, mvp, cameraMatrix)
        }
    }
    private fun drawUnitWithEffects(unit: PlacedObject, mvp: FloatArray, cameraMatrix: CameraMatrix) {
        // 检查绑定特效并分层
        val effects = GpuEffectSystem.getUnitEffects(unit)
        val underEffects = effects.filter { it.template.layer == EffectLayer.UNDER_UNITS }
        val overEffects = effects.filter { it.template.layer == EffectLayer.OVER_UNITS }

        // 1. 下特效
        for (eff in underEffects) {
            GpuEffectSystem.drawInstance(eff, cameraMatrix)
        }

        // 2. 单位本体
        drawUnit(unit, mvp)

        // 3. 上特效
        for (eff in overEffects) {
            GpuEffectSystem.drawInstance(eff, cameraMatrix)
        }
    }


    private fun drawUnit(unit: PlacedObject, mvp: FloatArray) {
        val texture = unit.textureBitmap
        if (texture != null && !texture.isRecycled) {
            drawTexturedUnit(unit, texture, mvp)
        } else {
            drawColoredShape(unit, mvp)
        }
    }

    private fun drawTexturedUnit(unit: PlacedObject, bitmap: Bitmap, mvp: FloatArray) {
        val shader = texturedShader ?: return
        shader.use()
        var glTexId = textureCache[bitmap]
        if (glTexId == null) {
            glTexId = createGlTexture(bitmap)
            textureCache[bitmap] = glTexId
        }
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTexId)
        shader.setInt("uTexture", 0)

        buildTexturedModelMatrix(unit)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, mvp, 0, modelMatrix, 0)
        shader.setMat4("uMVPMatrix", mvpMatrix)

        rectBuffer.position(0)
        val posHandle = shader.getAttribLocation("aPosition")
        GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, 0, rectBuffer)
        GLES30.glEnableVertexAttribArray(posHandle)

        texCoordBuffer.position(0)
        val texHandle = shader.getAttribLocation("aTexCoord")
        GLES30.glVertexAttribPointer(texHandle, 2, GLES30.GL_FLOAT, false, 0, texCoordBuffer)
        GLES30.glEnableVertexAttribArray(texHandle)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(posHandle)
        GLES30.glDisableVertexAttribArray(texHandle)
    }

    private fun drawColoredShape(unit: PlacedObject, mvp: FloatArray) {
        val shader = colorShader ?: return
        shader.use()
        val color = floatArrayOf(
            android.graphics.Color.red(unit.color) / 255f,
            android.graphics.Color.green(unit.color) / 255f,
            android.graphics.Color.blue(unit.color) / 255f,
            1f
        )
        shader.setVec4("uColor", color)

        buildShapeModelMatrix(unit)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, mvp, 0, modelMatrix, 0)
        shader.setMat4("uMVPMatrix", mvpMatrix)

        when (unit.shape) {
            "circle" -> {
                circleBuffer.position(0)
                val posHandle = shader.getAttribLocation("aPosition")
                GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, 0, circleBuffer)
                GLES30.glEnableVertexAttribArray(posHandle)
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, (circleVertices.size / 3))
                GLES30.glDisableVertexAttribArray(posHandle)
            }
            else -> {
                rectBuffer.position(0)
                val posHandle = shader.getAttribLocation("aPosition")
                GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, 0, rectBuffer)
                GLES30.glEnableVertexAttribArray(posHandle)
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
                GLES30.glDisableVertexAttribArray(posHandle)
            }
        }
    }

    private fun buildTexturedModelMatrix(unit: PlacedObject) {
        val drawHeight = if (unit.textureDisplayHeight > 0f) unit.textureDisplayHeight else unit.height.toFloat()
        val tex = unit.textureBitmap ?: return
        val texRatio = tex.width.toFloat() / tex.height.toFloat()
        val drawWidth = drawHeight * texRatio

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, unit.worldX, unit.worldY, 0f)

        // ★ 普通贴图单位：heading + 90°（贴图原始朝东）；导弹：直接用 heading
        val angle = if (unit.category == "missile") unit.heading else unit.heading - 90f
        Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f)

        Matrix.scaleM(modelMatrix, 0, drawWidth, drawHeight, 1f)
    }

    private fun buildShapeModelMatrix(unit: PlacedObject) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, unit.worldX, unit.worldY, 0f)
        Matrix.rotateM(modelMatrix, 0, unit.heading, 0f, 0f, 1f)
        when (unit.shape) {
            "circle" -> {
                val r = unit.size / 2f
                Matrix.scaleM(modelMatrix, 0, r * 2f, r * 2f, 1f)
            }
            "square" -> {
                val half = unit.size / 2f
                Matrix.scaleM(modelMatrix, 0, half * 2f, half * 2f, 1f)
            }
            else -> {
                Matrix.scaleM(modelMatrix, 0, unit.width.toFloat(), unit.height.toFloat(), 1f)
            }
        }
    }

    private fun createGlTexture(bitmap: Bitmap): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val id = textures[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        return id
    }

    fun release() {
        for (id in textureCache.values) {
            GLES30.glDeleteTextures(1, intArrayOf(id), 0)
        }
        textureCache.clear()
    }
}