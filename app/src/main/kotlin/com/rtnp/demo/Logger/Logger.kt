// app/src/main/kotlin/com/rtnp/demo/logger/Logger.kt
package com.rtnp.demo.logger

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

object Logger {

    enum class Level(val emoji: String, val priority: Int) {
        VERBOSE("⚪", Log.VERBOSE),
        DEBUG("🟢", Log.DEBUG),
        INFO("🔵", Log.INFO),
        WARNING("🟡", Log.WARN),
        ERROR("🔴", Log.ERROR)
    }

    private const val TARGET_LOG_DIR = "/storage/emulated/0/RTNP/logs"
    private const val MAX_SINGLE_MSG_LENGTH = 4000

    private var isInitialized = false
    private var logLevel: Level = Level.DEBUG

    // ★ 改用 FileOutputStream
    private var externalStream: FileOutputStream? = null
    private var privateStream: FileOutputStream? = null

    private val executor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

    fun init(context: Context, level: Level = Level.DEBUG) {
        if (isInitialized) return
        isInitialized = true
        logLevel = level

        val timestamp = fileDateFormat.format(Date())

        // 1. 外部存储
        try {
            val externalDir = File(TARGET_LOG_DIR)
            if (externalDir.exists() || externalDir.mkdirs()) {
                val file = File(externalDir, "game_$timestamp.log")
                externalStream = FileOutputStream(file, true)
                externalStream!!.write("=== 外部日志开始 ===\n".toByteArray())
                externalStream!!.flush()
            }
        } catch (_: Exception) {}

        // 2. 私有目录（保底）
        try {
            val privateDir = File(context.getExternalFilesDir(null), "logs").also { it.mkdirs() }
            val file = File(privateDir, "game_$timestamp.log")
            privateStream = FileOutputStream(file, true)
            privateStream!!.write("=== 私有日志开始 ===\n".toByteArray())
            privateStream!!.flush()
        } catch (_: Exception) {}

        // 崩溃捕获
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            e("CRASH", "线程: ${thread.name}", throwable)
            closeAll()
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    @JvmStatic fun v(tag: String, msg: String, t: Throwable? = null) = log(Level.VERBOSE, tag, msg, t)
    @JvmStatic fun d(tag: String, msg: String, t: Throwable? = null) = log(Level.DEBUG, tag, msg, t)
    @JvmStatic fun i(tag: String, msg: String, t: Throwable? = null) = log(Level.INFO, tag, msg, t)
    @JvmStatic fun w(tag: String, msg: String, t: Throwable? = null) = log(Level.WARNING, tag, msg, t)
    @JvmStatic fun e(tag: String, msg: String, t: Throwable? = null) = log(Level.ERROR, tag, msg, t)

    private fun log(level: Level, tag: String, message: String, throwable: Throwable?) {
        if (!isInitialized || level.priority < logLevel.priority) return

        val timestamp = dateFormat.format(Date())
        val threadName = Thread.currentThread().name
        val baseMsg = "[$timestamp][${level.emoji} $level][$threadName][$tag] $message"
        val fullMsg = if (throwable != null) {
            baseMsg + "\n" + getStackTraceString(throwable)
        } else baseMsg

        val truncatedMsg = if (fullMsg.length > MAX_SINGLE_MSG_LENGTH) {
            fullMsg.substring(0, MAX_SINGLE_MSG_LENGTH) + "...(截断)"
        } else fullMsg

        val bytes = (truncatedMsg + "\n").toByteArray()

        // ★ 每写一条立即同步到磁盘，不用缓冲区
        executor.execute {
            try { externalStream?.let { it.write(bytes); it.flush(); it.fd.sync() } } catch (_: Exception) {}
            try { privateStream?.let { it.write(bytes); it.flush(); it.fd.sync() } } catch (_: Exception) {}
        }
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }

    private fun closeAll() {
        try { externalStream?.close() } catch (_: Exception) {}
        try { privateStream?.close() } catch (_: Exception) {}
    }

    @JvmStatic
    fun shutdown() {
        isInitialized = false
        executor.execute { closeAll() }
    }
}