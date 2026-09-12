package com.rtnp.demo.gpu

import android.opengl.GLES30
import android.util.Log

class ShaderProgram(private val programHandle: Int) {

    companion object {
        private const val TAG = "ShaderProgram"

        fun create(vertexSource: String, fragmentSource: String): ShaderProgram? {
            val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
            if (vertexShader == 0) return null

            val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
            if (fragmentShader == 0) {
                GLES30.glDeleteShader(vertexShader)
                return null
            }

            val program = GLES30.glCreateProgram()
            GLES30.glAttachShader(program, vertexShader)
            GLES30.glAttachShader(program, fragmentShader)
            GLES30.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val log = GLES30.glGetProgramInfoLog(program)
                Log.e(TAG, "Program link failed: $log")
                // ★ 输出源码片段以便调试
                Log.e(TAG, "=== Vertex Shader ===\n$vertexSource\n=== Fragment Shader ===\n$fragmentSource")
                GLES30.glDeleteProgram(program)
                return null
            }

            GLES30.glDetachShader(program, vertexShader)
            GLES30.glDetachShader(program, fragmentShader)
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)

            return ShaderProgram(program)
        }

        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, source)
            GLES30.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val log = GLES30.glGetShaderInfoLog(shader)
                Log.e(TAG, "Shader compile failed (type=$type): $log")
                Log.e(TAG, "Shader source:\n$source")
                GLES30.glDeleteShader(shader)
                return 0
            }
            return shader
        }
    }

    fun use() { GLES30.glUseProgram(programHandle) }
    fun getUniformLocation(name: String): Int = GLES30.glGetUniformLocation(programHandle, name)
    fun setMat4(name: String, matrix: FloatArray) {
        GLES30.glUniformMatrix4fv(getUniformLocation(name), 1, false, matrix, 0)
    }
    fun setVec4(name: String, value: FloatArray) {
        val location = getUniformLocation(name)
        Log.d("ShaderProgram", "setVec4 $name at $location = ${value.joinToString()}")
        GLES30.glUniform4fv(location, 1, value, 0)
    }
    fun setFloat(name: String, value: Float) {
        val location = getUniformLocation(name)
        if (location == -1) {
            Log.e("ShaderProgram", "Uniform $name not found")
        }
        GLES30.glUniform1f(location, value)
    }
    fun setInt(name: String, value: Int) {
        GLES30.glUniform1i(getUniformLocation(name), value)
    }
    fun getAttribLocation(name: String): Int = GLES30.glGetAttribLocation(programHandle, name)
    fun delete() { GLES30.glDeleteProgram(programHandle) }
    fun setVec2(name: String, value: FloatArray) {
        val location = getUniformLocation(name)
        GLES30.glUniform2fv(location, 1, value, 0)
    }
    fun setVec3(name: String, value: FloatArray) {
        val location = getUniformLocation(name)
        GLES30.glUniform3fv(location, 1, value, 0)
    }
}