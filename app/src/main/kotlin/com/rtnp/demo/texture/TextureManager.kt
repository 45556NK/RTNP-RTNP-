package com.rtnp.demo.texture

import android.content.Context
import android.graphics.Bitmap
import com.rtnp.demo.image.ImageManager

/**
 * 全局纹理管理器
 * 
 * 职责：
 * - 扫描并注册 assets 下所有有效纹理
 * - 管理名称 → 编号 → 元数据的映射
 * - 提供 Bitmap 引用（Canvas用）
 * - 不直接操作 OpenGL，GL纹理的创建由调用方负责
 */
object TextureManager {

    private val textureMap = mutableMapOf<String, TextureMeta>()  // 名称 → 元数据
    private val idMap = mutableMapOf<Int, TextureMeta>()          // 编号 → 元数据
    private val pathMap = mutableMapOf<String, TextureMeta>()     // 路径 → 元数据（兼容旧调用）

    private var nextId = 0
    private var initialized = false

    data class TextureMeta(
        val id: Int,
        val name: String,
        val assetPath: String,
        val bitmap: Bitmap?,
        var gpuTextureId: Int = -1  // -1 表示尚未创建
    )

    fun init(context: Context) {
        if (initialized) return
        initialized = true

        val imgMgr = ImageManager.getInstance(context)
        val assetPaths = listAllImageFiles(context)

        for (path in assetPaths) {
            val name = extractName(path)
            if (textureMap.containsKey(name)) continue  // 跳过重名

            val bitmap = imgMgr.getBitmap(path)
            val meta = TextureMeta(
                id = nextId,
                name = name,
                assetPath = path,
                bitmap = bitmap
            )
            textureMap[name] = meta
            idMap[nextId] = meta
            pathMap[path] = meta
            nextId++
        }
    }

    /** 根据名称获取编号 */
    fun getIdByName(name: String): Int {
        return textureMap[name]?.id ?: -1
    }

    /** 根据编号获取编号（无操作，方便接口统一） */
    fun getIdById(id: Int): Int = if (idMap.containsKey(id)) id else -1

    /** 根据编号获取元数据 */
    fun getMeta(id: Int): TextureMeta? = idMap[id]

    /** 根据名称获取元数据 */
    fun getMeta(name: String): TextureMeta? = textureMap[name]

    /** 根据路径获取元数据（兼容旧调用） */
    fun getMetaByPath(path: String): TextureMeta? = pathMap[path]

    /** 获取 Bitmap（兼容旧调用） */
    fun getBitmap(id: Int): Bitmap? = idMap[id]?.bitmap

    fun getBitmap(name: String): Bitmap? = textureMap[name]?.bitmap

    fun getBitmapByPath(path: String): Bitmap? = pathMap[path]?.bitmap

    /**
     * 获取原始分辨率 Bitmap（不缩放，绕过 ImageManager 的 maxTextureSize 限制）
     * 适用于背景图、高清 UI 等需要原始画质的场景
     */
    fun getOriginalBitmap(context: Context, assetPath: String): Bitmap? {
        return try {
            context.assets.open(assetPath).use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 获取或创建 GPU 纹理 ID（使用原图）
     * 调用此方法会强制加载原始分辨率图片
     */
    fun getOrCreateGpuTextureOriginal(id: Int, context: Context, creator: (Bitmap) -> Int): Int {
        val meta = idMap[id] ?: return -1
        if (meta.gpuTextureId == -1) {
            val originalBitmap = getOriginalBitmap(context, meta.assetPath)
            if (originalBitmap != null) {
                meta.gpuTextureId = creator(originalBitmap)
            }
        }
        return meta.gpuTextureId
    }

    /** 获取或创建 GPU 纹理 ID（由 GPU 渲染器调用） */
    fun getOrCreateGpuTexture(id: Int, creator: (Bitmap) -> Int): Int {
        val meta = idMap[id] ?: return -1
        if (meta.gpuTextureId == -1 && meta.bitmap != null) {
            meta.gpuTextureId = creator(meta.bitmap)
        }
        return meta.gpuTextureId
    }

    /** 获取所有已注册的纹理名称 */
    fun getAllNames(): Set<String> = textureMap.keys

    /** 获取所有编号 */
    fun getAllIds(): Set<Int> = idMap.keys

    private fun listAllImageFiles(context: Context): List<String> {
        val result = mutableListOf<String>()
        try {
            scanDirectory(context, "images", result)
        } catch (_: Exception) {}
        return result
    }

    private fun scanDirectory(context: Context, dir: String, result: MutableList<String>) {
        try {
            val files = context.assets.list(dir) ?: return
            for (file in files) {
                val fullPath = "$dir/$file"
                try {
                    val subFiles = context.assets.list(fullPath)
                    if (subFiles != null && subFiles.isNotEmpty()) {
                        scanDirectory(context, fullPath, result)
                    } else {
                        if (isImageFile(file)) {
                            result.add(fullPath)
                        }
                    }
                } catch (_: Exception) {
                    if (isImageFile(file)) {
                        result.add(fullPath)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun isImageFile(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".png") || lower.endsWith(".jpg") ||
               lower.endsWith(".jpeg") || lower.endsWith(".webp") ||
               lower.endsWith(".bmp")
    }

    private fun extractName(path: String): String {
        val fileName = path.substringAfterLast("/").substringBeforeLast(".")
        return fileName
    }
}