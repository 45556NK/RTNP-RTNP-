package com.rtnp.demo.gpu.effect

import android.content.Context
import com.rtnp.demo.logger.Logger

object ShaderToolRegistry {

    private val fragmentShaders = mutableMapOf<String, String>()
    private val vertexShaders = mutableMapOf<String, String>()

    fun init(context: Context) {
        fragmentShaders.clear()
        vertexShaders.clear()
        scanShaders(context, "shaders")
        Logger.d("ShaderToolRegistry", "已加载着色器工具: fragment=${fragmentShaders.keys}, vertex=${vertexShaders.keys}")
    }

    fun getFragmentShader(id: String): String? = fragmentShaders[id]
    fun getVertexShader(id: String): String? = vertexShaders[id]
    fun getAllToolIds(): Set<String> = fragmentShaders.keys

    private fun scanShaders(context: Context, dir: String) {
        try {
            val files = context.assets.list(dir) ?: return
            for (file in files) {
                val fullPath = "$dir/$file"
                // ★ 先判断文件扩展名，再尝试作为目录扫描
                if (file.endsWith(".fsh")) {
                    val id = file.removeSuffix(".fsh")
                    try {
                        val source = context.assets.open(fullPath).bufferedReader().use { it.readText() }
                        fragmentShaders[id] = source
                        Logger.d("ShaderToolRegistry", "加载片段着色器: $id")
                    } catch (e: Exception) {
                        Logger.e("ShaderToolRegistry", "读取着色器失败: $fullPath", e)
                    }
                } else if (file.endsWith(".vsh")) {
                    val id = file.removeSuffix(".vsh")
                    try {
                        val source = context.assets.open(fullPath).bufferedReader().use { it.readText() }
                        vertexShaders[id] = source
                        Logger.d("ShaderToolRegistry", "加载顶点着色器: $id")
                    } catch (e: Exception) {
                        Logger.e("ShaderToolRegistry", "读取着色器失败: $fullPath", e)
                    }
                } else {
                    // 不是着色器文件，尝试作为子目录递归扫描
                    try {
                        val subFiles = context.assets.list(fullPath)
                        if (subFiles != null && subFiles.isNotEmpty()) {
                            scanShaders(context, fullPath)
                        }
                    } catch (_: Exception) {
                        // 既不是目录也不是着色器，忽略
                    }
                }
            }
        } catch (_: Exception) {
            Logger.e("ShaderToolRegistry", "扫描着色器目录失败: $dir")
        }
    }
}