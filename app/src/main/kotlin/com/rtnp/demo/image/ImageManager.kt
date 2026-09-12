// app/src/main/kotlin/com/rtnp/demo/image/ImageManager.kt
package com.rtnp.demo.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.PictureDrawable
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import java.io.IOException

class ImageManager private constructor(private val context: Context) {

    private val cache = mutableMapOf<String, Bitmap>()

    /** 贴图最大尺寸限制 */
    private val maxTextureSize = 512

    fun getBitmap(assetsPath: String): Bitmap? {
        cache[assetsPath]?.let { return it }

        return try {
            // ★ 判断是否为 SVG 文件
            if (assetsPath.lowercase().endsWith(".svg")) {
                loadSvgBitmap(assetsPath)
            } else {
                loadRasterBitmap(assetsPath)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 加载 SVG 文件并转换为 Bitmap
     */
    private fun loadSvgBitmap(assetsPath: String): Bitmap? {
        return try {
            context.assets.open(assetsPath).use { inputStream ->
                val svg = SVG.getFromInputStream(inputStream)
                
                // 获取 SVG 的原始尺寸
                val documentWidth = svg.documentWidth.toFloat()
                val documentHeight = svg.documentHeight.toFloat()
                
                // 计算缩放后的尺寸（限制在 maxTextureSize 内）
                val scale = if (documentWidth > maxTextureSize || documentHeight > maxTextureSize) {
                    minOf(maxTextureSize / documentWidth, maxTextureSize / documentHeight)
                } else {
                    1f
                }
                
                val width = (documentWidth * scale).toInt().coerceAtLeast(1)
                val height = (documentHeight * scale).toInt().coerceAtLeast(1)
                
                // 创建 Bitmap 并绘制 SVG
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.scale(scale, scale)
                
                val pictureDrawable = PictureDrawable(svg.renderToPicture())
                pictureDrawable.setBounds(0, 0, documentWidth.toInt(), documentHeight.toInt())
                pictureDrawable.draw(canvas)
                
                bitmap.also { cache[assetsPath] = it }
            }
        } catch (e: SVGParseException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 加载普通位图文件
     */
    private fun loadRasterBitmap(assetsPath: String): Bitmap? {
        return try {
            context.assets.open(assetsPath).use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)

                val originalWidth = options.outWidth
                val originalHeight = options.outHeight

                val bitmap = if (originalWidth > maxTextureSize || originalHeight > maxTextureSize) {
                    val scaleWidth = maxTextureSize.toFloat() / originalWidth
                    val scaleHeight = maxTextureSize.toFloat() / originalHeight
                    val scale = minOf(scaleWidth, scaleHeight)

                    val newWidth = (originalWidth * scale).toInt()
                    val newHeight = (originalHeight * scale).toInt()

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = calculateInSampleSize(originalWidth, originalHeight, newWidth, newHeight)
                    }

                    context.assets.open(assetsPath).use { secondStream ->
                        val decoded = BitmapFactory.decodeStream(secondStream, null, decodeOptions)
                        if (decoded != null && (decoded.width != newWidth || decoded.height != newHeight)) {
                            val scaled = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(scaled)
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                isFilterBitmap = true
                                isDither = true
                            }
                            val matrix = Matrix()
                            matrix.setScale(
                                newWidth.toFloat() / decoded.width,
                                newHeight.toFloat() / decoded.height
                            )
                            canvas.drawBitmap(decoded, matrix, paint)
                            if (scaled != decoded) decoded.recycle()
                            scaled
                        } else decoded
                    }
                } else {
                    context.assets.open(assetsPath).use { secondStream ->
                        BitmapFactory.decodeStream(secondStream)
                    }
                }

                bitmap?.also { cache[assetsPath] = it }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // ========== 以下方法保持不变 ==========

    private fun calculateInSampleSize(
        originalWidth: Int, originalHeight: Int,
        targetWidth: Int, targetHeight: Int
    ): Int {
        var sampleSize = 1
        while (originalWidth / sampleSize > targetWidth * 2 ||
               originalHeight / sampleSize > targetHeight * 2) {
            sampleSize *= 2
        }
        return sampleSize
    }

    fun loadBitmapDirect(assetsPath: String): Bitmap? {
        return try {
            if (assetsPath.lowercase().endsWith(".svg")) {
                loadSvgBitmap(assetsPath)
            } else {
                context.assets.open(assetsPath).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            }
        } catch (e: IOException) {
            null
        }
    }

    fun clearCache() {
        for (bmp in cache.values) {
            if (!bmp.isRecycled) {
                bmp.recycle()
            }
        }
        cache.clear()
    }

    fun removeFromCache(assetsPath: String) {
        val bmp = cache.remove(assetsPath)
        if (bmp != null && !bmp.isRecycled) {
            bmp.recycle()
        }
    }

    fun hasCached(assetsPath: String): Boolean {
        return cache.containsKey(assetsPath)
    }

    companion object {
        @Volatile
        private var instance: ImageManager? = null

        @JvmStatic
        fun getInstance(context: Context): ImageManager {
            return instance ?: synchronized(this) {
                instance ?: ImageManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}