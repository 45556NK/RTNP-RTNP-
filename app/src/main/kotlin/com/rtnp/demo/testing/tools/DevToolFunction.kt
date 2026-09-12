// app/src/main/kotlin/com/rtnp/demo/testing/tools/DevToolFunction.kt
package com.rtnp.demo.testing.tools

interface DevToolFunction {
    val id: String
    val name: String
    val type: FunctionType

    fun execute()
    fun getDisplayValue(): String = ""
}

enum class FunctionType {
    BUTTON,
    TOGGLE,
    SLIDER,
    INPUT   // ★ 新增：输入框类型
}