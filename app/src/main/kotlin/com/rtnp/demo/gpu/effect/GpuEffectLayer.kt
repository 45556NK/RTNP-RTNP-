package com.rtnp.demo.gpu.effect

import android.opengl.GLES30
import android.opengl.Matrix
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

class GpuEffectLayer(private val layer: EffectLayer) {

    internal val instances = mutableListOf<GpuEffectInstance>()
    private val programCache = mutableMapOf<String, ShaderProgram>()

    private val quadVertices = floatArrayOf(
        -1f,  1f, 0f,      0f, 0f,
        -1f, -1f, 0f,      0f, 1f,
         1f,  1f, 0f,      1f, 0f,
         1f, -1f, 0f,      1f, 1f
    )
    private val quadBuffer: FloatBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(quadVertices)
        .apply { position(0) }

    fun addInstance(instance: GpuEffectInstance) {
        instances.add(instance)
    }

    fun update(deltaTime: Float) {
        instances.forEach { instance ->
            instance.elapsed += deltaTime
            if (instance.template.followHost && instance.hostUnit != null) {
                val host = instance.hostUnit!!
                val params = instance.customParams
                val offsetX = (params?.get("offsetX") as? Float) ?: 0f
                val offsetY = (params?.get("offsetY") as? Float) ?: 0f
                val headingRad = Math.toRadians(host.heading.toDouble())
                val cosH = cos(headingRad).toFloat()
                val sinH = sin(headingRad).toFloat()
                instance.worldX = host.worldX + offsetX * cosH - offsetY * sinH
                instance.worldY = host.worldY + offsetX * sinH + offsetY * cosH
            }
        }
        instances.removeAll { !it.template.isAlive(it) }
    }

    fun draw(cameraMatrix: CameraMatrix) {
        val independent = instances.filter { !it.template.followUnit }
        if (independent.isEmpty()) return

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)

        val grouped = independent.groupBy { it.template }
        val mvp = cameraMatrix.getMVPMatrix()
        val identity = FloatArray(16)
        Matrix.setIdentityM(identity, 0)

        for ((template, group) in grouped) {
            val program = getOrCreateProgram(template) ?: continue
            program.use()

            for (instance in group) {
                val bounds = if (template.useWorldSpace) template.getWorldBounds(instance) else null

                if (bounds != null) {
                    val host = instance.hostUnit

                    if (host != null && template.followHost) {
                        val size = (bounds[2] - bounds[0]) * 0.5f
                        val half = size
                        val headingRad = Math.toRadians((host.heading + 90.0).toDouble())
                        val cosH = cos(headingRad).toFloat()
                        val sinH = sin(headingRad).toFloat()
                        val cx = instance.worldX
                        val cy = instance.worldY

                        val rotatedVertices = floatArrayOf(
                            cx + (-half * cosH - (-half) * sinH), cy + (-half * sinH + (-half) * cosH), 0f, 0f, 0f,
                            cx + (-half * cosH - half * sinH),    cy + (-half * sinH + half * cosH),    0f, 0f, 1f,
                            cx + (half * cosH - (-half) * sinH),  cy + (half * sinH + (-half) * cosH),  0f, 1f, 0f,
                            cx + (half * cosH - half * sinH),     cy + (half * sinH + half * cosH),     0f, 1f, 1f
                        )

                        val buf = ByteBuffer.allocateDirect(rotatedVertices.size * 4)
                            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(rotatedVertices).apply { position(0) }
                        program.setMat4("uMVPMatrix", mvp)

                        val params = template.process(instance.customParams ?: emptyMap(), instance).toMutableMap()
                        if (!params.containsKey("u_center")) {
                            params["u_center"] = floatArrayOf(0.5f, 0.5f)
                        }
                        if (!params.containsKey("u_aspect")) {
                            params["u_aspect"] = 1.0f
                        }
                        applyUniforms(program, params)
                        drawQuad(program, buf, GLES30.GL_TRIANGLE_STRIP, 4, true)
                    } else {
                        val minX = bounds[0]; val minY = bounds[1]
                        val maxX = bounds[2]; val maxY = bounds[3]
                        val vertices = floatArrayOf(
                            minX, minY, 0f,  0f, 0f,
                            minX, maxY, 0f,  0f, 1f,
                            maxX, minY, 0f,  1f, 0f,
                            maxX, maxY, 0f,  1f, 1f
                        )
                        val buf = ByteBuffer.allocateDirect(vertices.size * 4)
                            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).apply { position(0) }
                        program.setMat4("uMVPMatrix", mvp)

                        val params = template.process(instance.customParams ?: emptyMap(), instance).toMutableMap()
                        if (!params.containsKey("u_center")) {
                            params["u_center"] = floatArrayOf(0.5f, 0.5f)
                        }
                        if (!params.containsKey("u_aspect")) {
                            params["u_aspect"] = 1.0f
                        }
                        applyUniforms(program, params)
                        drawQuad(program, buf, GLES30.GL_TRIANGLE_STRIP, 4, true)
                    }
                } else {
                    program.setMat4("uMVPMatrix", identity)

                    val worldPos = floatArrayOf(instance.worldX, instance.worldY, 0f, 1f)
                    val ndcPos = FloatArray(4)
                    Matrix.multiplyMV(ndcPos, 0, mvp, 0, worldPos, 0)
                    val texCenterX = (ndcPos[0] + 1f) / 2f
                    val texCenterY = 1f - (ndcPos[1] + 1f) / 2f

                    val params = template.process(instance.customParams ?: emptyMap(), instance).toMutableMap()
                    if (!params.containsKey("u_center")) {
                        params["u_center"] = floatArrayOf(texCenterX, texCenterY)
                    }
                    if (!params.containsKey("u_aspect")) {
                        params["u_aspect"] = cameraMatrix.surfaceWidth.toFloat() / cameraMatrix.surfaceHeight.toFloat()
                    }
                    applyUniforms(program, params)
                    drawQuad(program, quadBuffer, GLES30.GL_TRIANGLE_STRIP, 4, true)
                }
            }
        }

        GLES30.glDepthMask(true)
    }

    fun drawInstance(instance: GpuEffectInstance, cameraMatrix: CameraMatrix) {
        val template = instance.template
        val program = getOrCreateProgram(template) ?: return
        program.use()

        val mvp = cameraMatrix.getMVPMatrix()
        val identity = FloatArray(16)
        Matrix.setIdentityM(identity, 0)

        val bounds = if (template.useWorldSpace) template.getWorldBounds(instance) else null

        if (bounds != null) {
            val host = instance.hostUnit
            val size = (bounds[2] - bounds[0]) * 0.5f
            val half = size
            val headingRad = Math.toRadians(((host?.heading ?: 0f) + 90.0).toDouble())
            val cosH = cos(headingRad).toFloat()
            val sinH = sin(headingRad).toFloat()
            val cx = instance.worldX
            val cy = instance.worldY

            val rotatedVertices = floatArrayOf(
                cx + (-half * cosH - (-half) * sinH), cy + (-half * sinH + (-half) * cosH), 0f, 0f, 0f,
                cx + (-half * cosH - half * sinH),    cy + (-half * sinH + half * cosH),    0f, 0f, 1f,
                cx + (half * cosH - (-half) * sinH),  cy + (half * sinH + (-half) * cosH),  0f, 1f, 0f,
                cx + (half * cosH - half * sinH),     cy + (half * sinH + half * cosH),     0f, 1f, 1f
            )
            val buf = ByteBuffer.allocateDirect(rotatedVertices.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer().put(rotatedVertices).apply { position(0) }
            program.setMat4("uMVPMatrix", mvp)

            val params = template.process(instance.customParams ?: emptyMap(), instance).toMutableMap()
            if (!params.containsKey("u_center")) {
                params["u_center"] = floatArrayOf(0.5f, 0.5f)
            }
            if (!params.containsKey("u_aspect")) {
                params["u_aspect"] = 1.0f
            }
            applyUniforms(program, params)
            drawQuad(program, buf, GLES30.GL_TRIANGLE_STRIP, 4, true)
        } else {
            program.setMat4("uMVPMatrix", identity)
            val worldPos = floatArrayOf(instance.worldX, instance.worldY, 0f, 1f)
            val ndcPos = FloatArray(4)
            Matrix.multiplyMV(ndcPos, 0, mvp, 0, worldPos, 0)
            val texCenterX = (ndcPos[0] + 1f) / 2f
            val texCenterY = 1f - (ndcPos[1] + 1f) / 2f

            val params = template.process(instance.customParams ?: emptyMap(), instance).toMutableMap()
            if (!params.containsKey("u_center")) {
                params["u_center"] = floatArrayOf(texCenterX, texCenterY)
            }
            if (!params.containsKey("u_aspect")) {
                params["u_aspect"] = cameraMatrix.surfaceWidth.toFloat() / cameraMatrix.surfaceHeight.toFloat()
            }
            applyUniforms(program, params)
            drawQuad(program, quadBuffer, GLES30.GL_TRIANGLE_STRIP, 4, true)
        }
    }

    private fun getOrCreateProgram(template: GpuEffectTemplate): ShaderProgram? {
        val toolId = template.shaderToolId
        programCache[toolId]?.let { return it }

        val fragSource = ShaderToolRegistry.getFragmentShader(toolId)
        val vertSource = ShaderToolRegistry.getVertexShader(toolId)

        if (fragSource != null) {
            val vs = vertSource ?: defaultVertexShader()
            val program = ShaderProgram.create(vs, fragSource)
            if (program != null) {
                programCache[toolId] = program
                return program
            }
        }
        return null
    }

    private fun defaultVertexShader() = """
        #version 300 es
        layout(location = 0) in vec4 aPosition;
        layout(location = 1) in vec2 aTexCoord;
        out vec2 textureCoordinate;
        uniform mat4 uMVPMatrix;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            textureCoordinate = aTexCoord;
        }
    """.trimIndent()

    private fun applyUniforms(program: ShaderProgram, params: Map<String, Any>) {
        for ((key, value) in params) {
            when (value) {
                is Float -> program.setFloat(key, value)
                is Int -> program.setInt(key, value)
                is FloatArray -> {
                    when (value.size) {
                        2 -> GLES30.glUniform2fv(program.getUniformLocation(key), 1, value, 0)
                        3 -> GLES30.glUniform3fv(program.getUniformLocation(key), 1, value, 0)
                        4 -> program.setVec4(key, value)
                        16 -> program.setMat4(key, value)
                    }
                }
            }
        }
    }

    private fun drawQuad(program: ShaderProgram, vertexBuffer: FloatBuffer, mode: Int, count: Int, hasTexCoord: Boolean) {
        vertexBuffer.position(0)
        val stride = 5 * 4
        val posHandle = program.getAttribLocation("aPosition")
        if (posHandle != -1) {
            GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, stride, vertexBuffer)
            GLES30.glEnableVertexAttribArray(posHandle)
        }
        if (hasTexCoord) {
            val texHandle = program.getAttribLocation("aTexCoord")
            if (texHandle != -1) {
                vertexBuffer.position(3)
                GLES30.glVertexAttribPointer(texHandle, 2, GLES30.GL_FLOAT, false, stride, vertexBuffer)
                GLES30.glEnableVertexAttribArray(texHandle)
            }
        }
        GLES30.glDrawArrays(mode, 0, count)
        if (posHandle != -1) GLES30.glDisableVertexAttribArray(posHandle)
        if (hasTexCoord) {
            val texHandle = program.getAttribLocation("aTexCoord")
            if (texHandle != -1) GLES30.glDisableVertexAttribArray(texHandle)
        }
    }
}