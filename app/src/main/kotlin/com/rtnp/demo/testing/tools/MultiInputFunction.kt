// app/src/main/kotlin/com/rtnp/demo/testing/tools/MultiInputFunction.kt
package com.rtnp.demo.testing.tools

import android.text.InputType

/**
 * 多条件输入功能接口
 */
interface MultiInputFunction : DevToolFunction {
    /** 获取多页输入定义（默认空列表） */
    fun getPages(): List<InputPage> = emptyList()

    /** 所有页面输入完成后调用，返回下一个要打开的功能或 null */
    fun onComplete(values: List<String>): MultiInputFunction? = null

    /** 获取功能标题 */
    fun getTitle(): String

    // 兼容旧接口
    fun getInputFields(): List<InputField> = emptyList()
}

/**
 * 输入页面定义
 */
data class InputPage(
    val title: String,
    val fields: List<InputField>
)

/**
 * 输入框定义
 */
data class InputField(
    val label: String,
    val defaultValue: String,
    val inputType: Int = InputType.TYPE_CLASS_TEXT,
    val hint: String = "",
    val choices: List<String>? = null
)