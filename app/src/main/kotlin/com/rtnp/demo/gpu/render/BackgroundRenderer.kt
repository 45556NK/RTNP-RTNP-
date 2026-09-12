package com.rtnp.demo.gpu.render

import android.opengl.GLES30
import android.opengl.Matrix
import com.rtnp.demo.gpu.GpuTextureProvider
import com.rtnp.demo.gpu.ShaderProgram
import com.rtnp.demo.texture.TextureManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class BackgroundRenderPass(
    private val textureProvider: GpuTextureProvider
) {
    private var shader: ShaderProgram? = null
    private var vertexBuffer: FloatBuffer? = null
    private var texCoordBuffer: FloatBuffer? = null

    private var screenWidth: Int = 1
    private var screenHeight: Int = 1
    private var context: android.content.Context? = null

    private val projectionMatrix = FloatArray(16)

    // 缓存原图尺寸
    private var cachedTexWidth: Float = 0f
    private var cachedTexHeight: Float = 0f
    private var textureSizeCached = false

    private val texCoords = floatArrayOf(
        0f, 0f,
        0f, 1f,
        1f, 0f,
        1f, 1f
    )

    fun setContext(context: android.content.Context) {
        this.context = context
    }

    fun init() {
        val vertexShaderCode = """
            #version 300 es
            layout(location = 0) in vec4 aPosition;
            layout(location = 1) in vec2 aTexCoord;
            out vec2 vTexCoord;
            uniform mat4 uProjection;
            void main() {
                gl_Position = uProjection * aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            #version 300 es
            precision mediump float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            uniform sampler2D uTexture;
            uniform float uAlpha;
            void main() {
                fragColor = texture(uTexture, vTexCoord);
                fragColor.a *= uAlpha;
            }
        """.trimIndent()

        shader = ShaderProgram.create(vertexShaderCode, fragmentShaderCode)
            ?: throw RuntimeException("Failed to create background shader")

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords)
        texCoordBuffer?.position(0)
    }

    fun setScreenSize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        Matrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)
    }

    fun draw(backgroundTextureName: String) {
        val ctx = context ?: return
        val textureId = TextureManager.getIdByName(backgroundTextureName)
        if (textureId == -1) return

        val meta = TextureManager.getMeta(textureId) ?: return

        // ★ 只在第一次加载原图并创建GPU纹理
        if (meta.gpuTextureId == -1 || !textureSizeCached) {
            val originalBitmap = TextureManager.getOriginalBitmap(ctx, meta.assetPath)
            if (originalBitmap != null) {
                meta.gpuTextureId = textureProvider.createGlTexture(originalBitmap)
                cachedTexWidth = originalBitmap.width.toFloat()
                cachedTexHeight = originalBitmap.height.toFloat()
                textureSizeCached = true
            } else {
                return
            }
        }

        val glTextureId = meta.gpuTextureId
        if (glTextureId == -1 || cachedTexWidth <= 0f || cachedTexHeight <= 0f) return

        // ★ 只在尺寸变化时重新计算顶点
        updateVerticesForContain(cachedTexWidth, cachedTexHeight)

        shader?.use()
        shader?.setMat4("uProjection", projectionMatrix)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glTextureId)
        shader?.setInt("uTexture", 0)
        shader?.setFloat("uAlpha", 0.85f)

        vertexBuffer?.let { buf ->
            buf.position(0)
            val posHandle = shader?.getAttribLocation("aPosition") ?: return
            GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, 0, buf)
            GLES30.glEnableVertexAttribArray(posHandle)
        }

        texCoordBuffer?.let { buf ->
            buf.position(0)
            val texHandle = shader?.getAttribLocation("aTexCoord") ?: return
            GLES30.glVertexAttribPointer(texHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
            GLES30.glEnableVertexAttribArray(texHandle)
        }

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

   private fun updateVerticesForContain(texWidth: Float, texHeight: Float) {
       val scaleW = screenWidth.toFloat() / texWidth
       val scaleH = screenHeight.toFloat() / texHeight
       // ★ 取 max：某一边贴合即停止，另一边超出则裁剪
       val scale = maxOf(scaleW, scaleH)

       val displayPixelW = texWidth * scale
       val displayPixelH = texHeight * scale

       val left = (screenWidth.toFloat() - displayPixelW) / 2f
       val top = (screenHeight.toFloat() - displayPixelH) / 2f
       val right = left + displayPixelW
       val bottom = top + displayPixelH

       val vertices = floatArrayOf(
           left,  top,    0f,
           left,  bottom, 0f,
           right, top,    0f,
           right, bottom, 0f
       )

       vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
           .order(ByteOrder.nativeOrder())
           .asFloatBuffer()
           .put(vertices)
       vertexBuffer?.position(0)
   }

}