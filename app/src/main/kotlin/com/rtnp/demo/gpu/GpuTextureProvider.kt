package com.rtnp.demo.gpu

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import com.rtnp.demo.texture.TextureManager

class GpuTextureProvider {

    fun getTextureId(textureId: Int): Int {
        return TextureManager.getOrCreateGpuTexture(textureId) { bitmap ->
            createGlTexture(bitmap)
        }
    }

    fun bindTexture(textureId: Int, textureUnit: Int = GLES30.GL_TEXTURE0) {
        val glId = getTextureId(textureId)
        if (glId == -1) return
        GLES30.glActiveTexture(textureUnit)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, glId)
    }

    fun releaseAll() {
        for (id in TextureManager.getAllIds()) {
            val meta = TextureManager.getMeta(id)
            if (meta != null && meta.gpuTextureId != -1) {
                val textures = intArrayOf(meta.gpuTextureId)
                GLES30.glDeleteTextures(1, textures, 0)
                meta.gpuTextureId = -1
            }
        }
    }

    fun createGlTexture(bitmap: Bitmap): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val textureId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)

        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        return textureId
    }
}